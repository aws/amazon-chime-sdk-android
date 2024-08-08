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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
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
 *
 *  Note that you must finish starting the foreground service before calling start. Otherwise start
 *  will fail to succeed despite having the user grant permission and despite having created
 *  a foreground service for the [MediaProjection]. [CaptureSourceObserver.onCaptureFailed] will be
 *  called with [CaptureSourceError.SystemFailure] in this case.
 *
 *  If the [MediaProjection] could not be obtained with the [activityResultCode] and [activityData],
 *  [CaptureSourceObserver.onCaptureFailed] will be called with [CaptureSourceError.ConfigurationFailure].
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
    private var targetResolution: VideoResolution = VideoResolution.VideoResolutionFHD
    private val screenCaptureResolutionCalculator: ScreenCaptureResolutionCalculator = ScreenCaptureResolutionCalculator()

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
    // resize when it changes to avoid the aforementioned issue.
    private var isOrientationInPortrait = true
    // This is used to block frames during resizing, and to avoid
    // calling observers when we are just resizing
    private var isResizingForOrientationChange = false

    private val TAG = "DefaultScreenCaptureSource"

    init {
        val thread = HandlerThread(TAG)
        thread.start()
        handler = Handler(thread.looper)
    }

    override fun start() {
        // This function is shared with logic which resizing following orientation changes, so post it
        // onto the handler for thread safety
        handler.post {
            val success = startInternal()

            // Set this to no-op any future resizing requests on the handler
            isResizingForOrientationChange = false

            if (success) {
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onCaptureStarted()
                }
            }
        }
    }
    override fun setMaxResolution(maxResolution: VideoResolution) {
        if (maxResolution == VideoResolution.VideoResolutionUHD) {
            targetResolution = VideoResolution.VideoResolutionUHD
        } else {
            targetResolution = VideoResolution.VideoResolutionFHD
        }
    }

    // Make an integer 16's multiples using bitwise and
    private fun alignNumberBy16(number: Int): Int {
        val maxIntAlignedBy16 = 0x7FFFFFF0
        return number and maxIntAlignedBy16
    }

    // Separate internal function for clearness, return true when success; must be called on handler
    private fun startInternal(): Boolean {
        if (mediaProjection != null) {
            logger.warn(TAG, "Screen capture has not been stopped before start request, stopping to release resources")
            stop()
        }
        logger.info(TAG, "Starting screen capture source")

        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(activityResultCode, activityData)
        } catch (exception: SecurityException) {
            logger.error(TAG, "Failed to retrieve media projection due to SecurityException. The foreground service may not have finished starting before start was called.")

            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.SystemFailure)
            }
            return false
        }

        if (mediaProjection == null) {
            logger.error(TAG, "Failed to retrieve media projection. The resultCode or data may have been invalid.")

            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
            }
            return false
        }

        isOrientationInPortrait = isOrientationInPortrait()
        val size = getAdjustedWidthAndHeight()
        val newSurfaceTextureSource = surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
            size.first,
            size.second,
            contentHint
        ).apply {
            minFps = MIN_FPS
            addVideoSink(this@DefaultScreenCaptureSource)
            start()
        }
        surfaceTextureSource = newSurfaceTextureSource

        mediaProjection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    // clean up resources
                    stopInternal()
                }
            },
            handler
        )
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            TAG,
            size.first,
            size.second,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surfaceTextureSource?.surface,
                object : VirtualDisplay.Callback() {
                    override fun onStopped() {
                        // Don't trigger observer for resizing
                        if (!isResizingForOrientationChange) {
                            ObserverUtils.notifyObserverOnMainThread(observers) {
                                it.onCaptureStopped()
                            }
                        }
                    }
                },
            handler
        )

        logger.info(TAG, "Media projection adapter activity succeeded, virtual display created")
        return true
    }

    private fun isOrientationInPortrait(): Boolean {
        // Note that these metrics depend on orientation
        displayMetrics = context.resources.displayMetrics
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: throw RuntimeException("No display found.")
        // Use `getRealMetrics` to properly account for menu, status bar, and rotation
        display.getRealMetrics(displayMetrics)

        val rotation = display.rotation
        val isInPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        logger.info(TAG, "isOrientationInPortrait: $isInPortrait")
        return isInPortrait
    }

    private fun getAdjustedWidthAndHeight(): Pair<Int, Int> {
        val displayWidthAndHeight = if (isOrientationInPortrait) {
            arrayOf(minOf(displayMetrics.widthPixels, displayMetrics.heightPixels), maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels))
        } else {
            arrayOf(maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels), minOf(displayMetrics.widthPixels, displayMetrics.heightPixels))
        }
        logger.info(TAG, "displayMetrics - width: ${displayWidthAndHeight[0]}, height: ${displayWidthAndHeight[1]}")

        val targetSize: IntArray = screenCaptureResolutionCalculator.computeTargetSize(displayWidthAndHeight[0], displayWidthAndHeight[1], targetResolution.width, targetResolution.height)
        val width: Int = screenCaptureResolutionCalculator.alignToEven(targetSize[0])
        val height: Int = screenCaptureResolutionCalculator.alignToEven(targetSize[1])

        // Note that in landscape, for some reason `getRealMetrics` doesn't account for the status bar correctly
        // so we try to account for it with a manual adjustment to the surface size to avoid letterboxes
        // Some android device H.264 encoder is not able to handle size that is not multiples of 16 (such as Pixel3),
        // so screenCapture surface size is aligned manually to avoid the issue
        val adjustedWidth = alignNumberBy16(width - (if (isOrientationInPortrait) 0 else getStatusBarHeight()))
        val adjustedHeight = alignNumberBy16(height)
        logger.info(TAG, "width: $adjustedWidth, adjustedHeight: $adjustedHeight")
        return Pair(adjustedWidth, adjustedHeight)
    }

    override fun stop() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            stopInternal()

            // Set this to no-op any future resizing requests on the handler
            isResizingForOrientationChange = false

            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureStopped()
            }
        }
    }

    // Separate internal function to be reused; must be called on handler
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
            isResizingForOrientationChange = true
            logger.info(TAG, "Orientation changed from ${if (this.isOrientationInPortrait) "portrait" else "landscape"} " +
                "to ${if (isOrientationInPortrait) "portrait" else "landscape"}, resize virtual display to update dimensions")
            // Post this task to avoid deadlock with the surface texture source handler
            handler.post {
                // Double check that start or stop hasn't been called since this was posted
                if (isResizingForOrientationChange) {
                    resize()
                    isResizingForOrientationChange = false
                }
            }
            return
        }

        // Ignore frames while we are recreating the surface and display
        if (isResizingForOrientationChange) return
        sinks.forEach { it.onVideoFrameReceived(frame) }
    }

    private fun resize() {
        isOrientationInPortrait = isOrientationInPortrait()
        val size = getAdjustedWidthAndHeight()
        logger.info(TAG, "resize to width: ${size.first}, height: ${size.second}")
        virtualDisplay?.surface?.release()
        surfaceTextureSource?.removeVideoSink(this@DefaultScreenCaptureSource)
        surfaceTextureSource?.stop()
        surfaceTextureSource?.release()
        surfaceTextureSource = null
        val newSurfaceTextureSource = surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
            size.first,
            size.second,
            contentHint
        ).apply {
            minFps = MIN_FPS
            addVideoSink(this@DefaultScreenCaptureSource)
            start()
        }
        surfaceTextureSource = newSurfaceTextureSource
        virtualDisplay?.resize(size.first, size.second, displayMetrics.densityDpi)
        virtualDisplay?.surface = newSurfaceTextureSource.surface
        logger.info(TAG, "resize done!")
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
