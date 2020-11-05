/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultGlVideoFrameDrawer
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.JniUtil

/**
 * [CpuVideoProcessor] draws frames to RGBA, converts to CPU, and then applies a simple black and white filter before forwarding
 */
class CpuVideoProcessor(private val logger: Logger, eglCoreFactory: EglCoreFactory) : VideoSource,
    VideoSink {
    // The camera capture source currently output OES texture frames, so we draw them to a frame buffer that
    // we can read to host memory from
    private val rectDrawer = DefaultGlVideoFrameDrawer()
    // Helper which wraps an OpenGLES texture frame buffer with resize capabilities
    private val textureFrameBuffer = GlTextureFrameBufferHelper(GLES20.GL_RGBA)

    override val contentHint: VideoContentHint = VideoContentHint.Motion

    // State necessary for EGL operations
    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread("DemoCpuVideoProcessor")
    private val handler: Handler

    // Downstream video sinks
    private val sinks = mutableSetOf<VideoSink>()

    private val DUMMY_PBUFFER_OFFSET = 0

    private val TAG = "DemoCpuVideoProcessor"

    init {
        thread.start()
        handler = Handler(thread.looper)

        handler.post {
            eglCore = eglCoreFactory.createEglCore()

            // We need to create a dummy surface before we can set the context as current
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

            logger.info(TAG, "Created demo CPU video processor")
        }
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        frame.retain()
        handler.post {
            // Note: This processor assumes that the incoming call will be on a valid EGL context
            textureFrameBuffer.setSize(frame.getRotatedWidth(), frame.getRotatedHeight())
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.frameBufferId)

            val matrix = Matrix()
            // Shift before flipping
            matrix.preTranslate(0.5f, 0.5f)
            // RGBA frames are upside down relative to texture coordinates
            matrix.preScale(1f, -1f)
            // Unshift following flip
            matrix.preTranslate(-0.5f, -0.5f)
            // Note the draw call will account for any rotation, so we need to account for that in viewport width/height
            rectDrawer.drawFrame(
                frame,
                0,
                0,
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                matrix
            )

            // Read RGBA data to native byte buffer
            val rgbaData = JniUtil.nativeAllocateByteBuffer(frame.width * frame.height * 4)
            GLES20.glReadPixels(
                0,
                0,
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                rgbaData
            )
            GlUtil.checkGlError("glReadPixels")

            val rgbaBuffer =
                VideoFrameRGBABuffer(
                    frame.getRotatedWidth(),
                    frame.getRotatedHeight(),
                    rgbaData, frame.getRotatedWidth() * 4,
                    Runnable { JniUtil.nativeFreeByteBuffer(rgbaData) })

            convertToBlackAndWhite(rgbaBuffer)

            val processedFrame = VideoFrame(frame.timestampNs, rgbaBuffer)
            frame.release()

            sinks.forEach { it.onVideoFrameReceived(processedFrame) }
            processedFrame.release()
        }
    }

    fun release() {
        handler.post {
            logger.info(TAG, "Releasing CPU video processor source")

            rectDrawer.release()
            textureFrameBuffer.release()
            eglCore.release()

            handler.looper.quit()
        }
    }

    // This is of course an extremely inefficient way of converting to black and white
    private fun convertToBlackAndWhite(rgbaBuffer: VideoFrameRGBABuffer) {
        // So we don't need to pollute with @ExperimentalUnsignedTypes annotation
        fun Byte.toPositiveInt() = toInt() and 0xFF

        for (x in 0 until rgbaBuffer.width) {
            for (y in 0 until rgbaBuffer.height) {
                val rLocation = y * rgbaBuffer.stride + x * 4
                val gLocation = rLocation + 1
                val bLocation = rLocation + 2

                val rValue = rgbaBuffer.data[rLocation].toPositiveInt()
                val gValue = rgbaBuffer.data[gLocation].toPositiveInt()
                val bValue = rgbaBuffer.data[bLocation].toPositiveInt()

                val newValue = ((rValue + gValue + bValue) / (3.0)).toByte()

                rgbaBuffer.data.put(rLocation, newValue)
                rgbaBuffer.data.put(gLocation, newValue)
                rgbaBuffer.data.put(bLocation, newValue)
            }
        }
    }
}
