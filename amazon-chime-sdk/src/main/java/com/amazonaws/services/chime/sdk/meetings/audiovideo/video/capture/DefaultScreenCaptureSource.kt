/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.content.Context
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlin.math.ceil
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultScreenCaptureSource] uses [MediaProjection] to create a [VirtualDisplay] to capture the
 * device screen. It will render the captured frames to a [Surface] provided by a [SurfaceTextureCaptureSourceFactory].
 *
 * Builders will need to get permission from users to obtain the [activityResultCode] and [activityData] arguments,
 * required to create an internal [MediaProjection] object.
 * Read [content share guide](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/content_share.md) for more information.
 */
class DefaultScreenCaptureSource(
    private val context: Context,
    private val logger: Logger,
    private val surfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory,
    private val activityResultCode: Int,
    private val activityData: Intent,
    private val displayManager: DisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager,
    private val mediaProjectionManager: MediaProjectionManager = context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
) : VideoCaptureSource, VideoSink {
    private lateinit var displayMetrics: DisplayMetrics
    private var virtualDisplay: VirtualDisplay? = null

    // This source provides a surface we pass into the system APIs
    // and then starts emitting frames once the system starts drawing to the
    // surface. To speed up restart, since theses sources have to wait on
    // in-flight frames to finish release, we just begin the release and
    // create a new one
    private var surfaceTextureSource: SurfaceTextureCaptureSource? = null
    // See minFPS documentation in SurfaceTextureCaptureSource for motivation
    private val MIN_FPS = 15

    // Note that we intentionally choose to not reuse MediaProjection
    // between stop and start, as there are often (likely Android) bugs
    // resulting in cropped/glitched captures, or stalled capture
    // when one MediaProjection is used multiple times with different surfaces
    // or surfaces which have changed shape
    //
    // Since these are quick to create, we just take the result code and
    // data from the MediaProjection activity and create it within the source
    private var mediaProjection: MediaProjection? = null
    private val handler: Handler

    private val observers = mutableSetOf<CaptureSourceObserver>()

    // Concurrency modification could happen when sink gets
    // added/removed from another thread while sending frames
    private val sinks = ConcurrentSet.createConcurrentSet<VideoSink>()

    // This will prioritize resolution over framerate
    override val contentHint = VideoContentHint.Text

    // MediaProjection will only draw frames with the correct rotation
    // which means if the surface is recreated, it will, for example
    // draw a WxH frame on a HxW surface (it uses aspect fit).
    //
    // Therefore to avoid letterboxes we must track orientation and
    // restart when it changes to avoid the aforementioned issue.
    private var isOrientationInPortrait = true
    // This is used to block frames during restart, and to avoid
    // calling observers when we are just restarting
    private var isRestartingForOrientationChange = false

    private val TAG = "DefaultScreenCaptureSource"

    init {
        val thread = HandlerThread(TAG)
        thread.start()
        handler = Handler(thread.looper)
    }

    override fun start() {
        // This function is shared with logic which restarts following orientation changes, so post it
        // onto the handler for thread safety
        handler.post {
            startInternal()

            // Set this to no-op any future restart requests on the handler
            isRestartingForOrientationChange = false

            // Notify here so restarts do not trigger the callback
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureStarted()
            }
        }
    }

    // Make an integer 16's multiples using bitwise and
    private fun alignNumberBy16(number: Int): Int {
        val maxIntAlignedBy16 = 0x7FFFFFF0
        return number and maxIntAlignedBy16
    }

    // Separate internal function since only some logic is shared between external calls
    // and internal restarts; must be called on handler
    private fun startInternal() {
        if (mediaProjection != null) {
            logger.warn(TAG, "Screen capture has not been stopped before start request, stopping to release resources")
            stop()
        }
        logger.info(TAG, "Starting screen capture source")

        mediaProjection = mediaProjectionManager.getMediaProjection(activityResultCode, activityData)
        if (mediaProjection == null) {
            logger.error(TAG, "Was not able to retrieve media projection, may have been passed invalid result code or data")
            return
        }

        // Note that these metrics depend on orientation
        displayMetrics = context.resources.displayMetrics
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: throw RuntimeException("No display found.")
        // Use `getRealMetrics` to properly account for menu, status bar, and rotation
        display.getRealMetrics(displayMetrics)

        val rotation = display.rotation
        isOrientationInPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180

        // Note that in landscape, for some reason `getRealMetrics` doesn't account for the status bar correctly
        // so we try to account for it with a manual adjustment to the surface size to avoid letterboxes
        // Some android device H.264 encoder is not able to handle size that is not multiples of 16 (such as Pixel3),
        // so screenCapture surface size is aligned manually to avoid the issue
        surfaceTextureSource =
            surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
                alignNumberBy16(displayMetrics.widthPixels - (if (isOrientationInPortrait) 0 else getStatusBarHeight())),
                alignNumberBy16(displayMetrics.heightPixels),
                contentHint
            )
        surfaceTextureSource?.minFps = MIN_FPS

        surfaceTextureSource?.addVideoSink(this)
        surfaceTextureSource?.start()

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            TAG,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surfaceTextureSource?.surface,
                object : VirtualDisplay.Callback() {
                    override fun onStopped() {
                        // Don't trigger observer for restart
                        if (!isRestartingForOrientationChange) {
                            ObserverUtils.notifyObserverOnMainThread(observers) {
                                it.onCaptureStopped()
                            }
                        }
                    }
                },
            handler
        )

        logger.info(TAG, "Media projection adapter activity succeeded, virtual display created")
    }

    override fun stop() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            stopInternal()

            // Set this to no-op any future restart requests on the handler
            isRestartingForOrientationChange = false

            // Notify here so restarts do not trigger the callback
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureStopped()
            }
        }
    }

    // Separate internal function since only some logic is shared between external calls
    // and internal restarts; must be called on handler
    private fun stopInternal() {
        logger.info(TAG, "Stopping screen capture source")

        mediaProjection?.stop()
        mediaProjection = null

        virtualDisplay?.release()
        virtualDisplay = null

        surfaceTextureSource?.removeVideoSink(this)
        surfaceTextureSource?.stop()
        surfaceTextureSource?.release()
        surfaceTextureSource = null
    }

    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.add(observer)
    }

    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.remove(observer)
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        // Check if display rotation has changed; since this is
        // not an activity there is otherwise no way to observe this
        // besides checking every frame
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: throw RuntimeException("No display found.")
        val rotation = display.rotation
        val isOrientationInPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        if (this.isOrientationInPortrait != isOrientationInPortrait) {
            isRestartingForOrientationChange = true
            logger.info(TAG, "Orientation changed from ${if (this.isOrientationInPortrait) "portrait" else "landscape"} " +
                "to ${if (isOrientationInPortrait) "portrait" else "landscape"}, restarting screen capture to update dimensions")
            // Post this task to avoid deadlock with the surface texture source handler
            handler.post {
                // Double check that start or stop hasn't been called since this was posted
                if (isRestartingForOrientationChange) {
                    stopInternal()
                    startInternal()
                    isRestartingForOrientationChange = false
                }
            }
            return
        }

        // Ignore frames while we are recreating the surface and display
        if (isRestartingForOrientationChange) return
        sinks.forEach { it.onVideoFrameReceived(frame) }
    }

    fun release() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Stopping handler looper")
            handler.removeCallbacksAndMessages(null)
            handler.looper.quit()
        }
    }

    // A combination of hardcoded methods to try to obtain or estimate the status bar height
    private fun getStatusBarHeight(): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            ceil((if (VERSION.SDK_INT >= VERSION_CODES.M) 24 else 25) * resources.displayMetrics.density).toInt()
        }
    }
}
