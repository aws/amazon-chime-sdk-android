/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.RenderScript
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
import java.security.InvalidParameterException

/**
 * [BackgroundBlurVideoFrameProcessor] Draws frames to RGBA, converts to CPU, identifies the
 * foreground person and blurs the background of a video frame.
 * @param logger: [Logger] - Logger to log the data.
 * @param eglCoreFactory: [EglCoreFactory] - Factory to create [EglCore] objects to hold EGL state.
 * @param context: [Context] - Context to create blur and segmentation processor.
 * @param configurations: [BackgroundBlurConfiguration] - BlurStrength - how much blur to apply to a frame. It
 * specifies blurValue that corresponds to blur radius used in gaussian blur. It
 * accepts a float value from 0 to 25, where higher the number more blurrier the image will be.
 */
class BackgroundBlurVideoFrameProcessor @JvmOverloads constructor(
    private val logger: Logger,
    private val eglCoreFactory: EglCoreFactory,
    context: Context,
    var configurations: BackgroundBlurConfiguration?
) : VideoSource, VideoSink {
    override val contentHint: VideoContentHint = VideoContentHint.Motion

    private val BLUR_MIN_SUPPORTED_RADIUS = 0.0f
    private val BLUR_MAX_SUPPORTED_RADIUS = 25.0f
    private val TAG = "BackgroundBlurVideoFrameProcessor"
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

    private val blurProcessor = BlurProcessor(RenderScript.create(context))

    init {
        validateConfigs()
        thread.start()
        handler = Handler(thread.looper)

        handler.post {
            eglCore = eglCoreFactory.createEglCore()

            // We need to create a dummy surface before we can set the context as current.
            val surfaceAttribs =
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
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

            val backgroundBlurredBitmap = getBackgroundBlurredBitmap(inputBitmap, frame)

            // Video frame with blurred background.
            val processedFrame = backgroundFilterVideoFrameProcessor.getProcessedFrame(
                frame,
                backgroundBlurredBitmap,
                rgbaData
            )
            frame.release()

            sinks.forEach { it.onVideoFrameReceived(processedFrame) }
            processedFrame.release()
            JniUtil.nativeFreeByteBuffer(rgbaData)
        }
    }

    fun getBackgroundBlurredBitmap(inputBitmap: Bitmap, frame: VideoFrame): Bitmap? {
        val scaledInputBitmap =
            backgroundFilterVideoFrameProcessor.getScaledInputBitmap(frame, inputBitmap)
        val outputBitmap =
            backgroundFilterVideoFrameProcessor.getSegmentationMask(scaledInputBitmap)

        // Blur image.
        configurations?.let {
            blurProcessor.initialize(
                scaledInputBitmap.width,
                scaledInputBitmap.height,
                it.blurStrength
            )
        }
        val blurredBitmap = blurProcessor.process(scaledInputBitmap)

        return backgroundFilterVideoFrameProcessor.drawImageWithMask(
            scaledInputBitmap,
            outputBitmap,
            blurredBitmap
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
            configurations = BackgroundBlurConfiguration()
        }
        configurations?.blurStrength?.let {
            if (it <= BLUR_MIN_SUPPORTED_RADIUS || it > BLUR_MAX_SUPPORTED_RADIUS) {
                val msg =
                    "Blur Strength should be in the range ($BLUR_MIN_SUPPORTED_RADIUS < blurStrength <= $BLUR_MAX_SUPPORTED_RADIUS)." +
                            "See https://developer.android.com/reference/android/renderscript/ScriptIntrinsicBlur#setRadius(float) for reference."
                throw InvalidParameterException(msg)
            }
        }
    }

    fun release() {
        handler.post {
            logger.info(TAG, "Releasing $TAG source")
            blurProcessor.release()
            backgroundFilterVideoFrameProcessor.release()
            eglCore.release()

            handler.looper.quit()
        }
    }
}
