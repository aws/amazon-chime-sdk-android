/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.os.Handler
import android.os.HandlerThread
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.BackgroundFilterVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.JniUtil

/**
 * [BackgroundReplacementVideoFrameProcessor] Draws frames to RGBA, converts to CPU, identifies the foreground person
 * and replaces the background of a video frame.
 * @param logger: [Logger] - Logger to log the data.
 * @param eglCoreFactory: [EglCoreFactory] - Factory to create [EglCore] objects to hold EGL state.
 * @param context: [Context] - Context to create segmentation processor.
 * @param configurations: [BackgroundReplacementConfiguration] - Image to replace the background with.
 */
class BackgroundReplacementVideoFrameProcessor @JvmOverloads constructor(
    private val logger: Logger,
    eglCoreFactory: EglCoreFactory,
    private val context: Context,
    var configurations: BackgroundReplacementConfiguration?
) : VideoSource, VideoSink {
    override val contentHint: VideoContentHint = VideoContentHint.Motion

    private val TAG = "BackgroundReplacementVideoFrameProcessor"
    private val DUMMY_PBUFFER_OFFSET = 0

    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread(TAG)
    private val handler: Handler

    private val sinks = ConcurrentSet.createConcurrentSet<VideoSink>()

    private val backgroundFilterVideoFrameProcessor = BackgroundFilterVideoFrameProcessor(
        logger,
        context,
        TAG
    )

    init {
        validateConfigs()
        thread.start()
        handler = Handler(thread.looper)

        handler.post {
            eglCore = eglCoreFactory.createEglCore()

            // We need to create a dummy surface before we can set the context as current.
            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            eglCore.eglSurface = EGL14.eglCreatePbufferSurface(
                eglCore.eglDisplay,
                eglCore.eglConfig,
                surfaceAttribs,
                DUMMY_PBUFFER_OFFSET
            )
            EGL14.eglMakeCurrent(
                eglCore.eglDisplay,
                eglCore.eglSurface,
                eglCore.eglSurface,
                eglCore.eglContext
            )
            GlUtil.checkGlError("Failed to set dummy surface to initialize surface texture video source")

            logger.info(TAG, "Created $TAG")
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        frame.retain()
        handler.post {
            val rgbaData =
                backgroundFilterVideoFrameProcessor.getByteBufferFromInputVideoFrame(frame)
            val inputBitmap = backgroundFilterVideoFrameProcessor.getInputBitmap(frame)
            val backgroundReplacedBitmap = getBackgroundReplacedBitmap(inputBitmap, frame)

            // Video frame with replaced background.
            val processedFrame = backgroundFilterVideoFrameProcessor.getProcessedFrame(
                frame,
                backgroundReplacedBitmap,
                rgbaData
            )
            frame.release()

            sinks.forEach { it.onVideoFrameReceived(processedFrame) }
            processedFrame.release()
            JniUtil.nativeFreeByteBuffer(rgbaData)
        }
    }

    fun getBackgroundReplacedBitmap(inputBitmap: Bitmap, frame: VideoFrame): Bitmap? {
        val scaledInputBitmap =
            backgroundFilterVideoFrameProcessor.getScaledInputBitmap(frame, inputBitmap)
        val maskedBitmap =
            backgroundFilterVideoFrameProcessor.getSegmentationMask(scaledInputBitmap)
        // When creating scaled bitmap, we set filter to true as it provides better image quality with smooth edges around persons boundary.
        val upScaledMaskedBitmap = maskedBitmap?.let { Bitmap.createScaledBitmap(it, frame.getRotatedWidth(), frame.getRotatedHeight(), true) }

        val replacementBitmap = configurations?.image?.let {
            Bitmap.createScaledBitmap(
                it,
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                false
            )
        }

        return backgroundFilterVideoFrameProcessor.drawImageWithMask(
            inputBitmap,
            upScaledMaskedBitmap,
            replacementBitmap
        )
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    private fun validateConfigs() {
        if (configurations == null) {
            configurations = BackgroundReplacementConfiguration()
        }
    }

    fun release() {
        handler.post {
            logger.info(TAG, "Releasing $TAG source")

            backgroundFilterVideoFrameProcessor.release()
            eglCore.release()

            handler.looper.quit()
        }
    }
}
