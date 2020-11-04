/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultGlVideoFrameDrawer
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [GpuVideoProcessor] is a simple demo processor which draws incoming frames onto a new surface with
 * a black and white filter. It also draws the original image into a corner of the screen.
 *
 * To pass along the image, it maintains its own internal OpenGLES texture (the texture which is drawn to)
 * which it will package as a [VideoFrame] and forward downstream, only updating the texture
 * (and creating the next [VideoFrame] to pass downstream) when the previous has been released.
 */
class GpuVideoProcessor(private val logger: Logger, eglCoreFactory: EglCoreFactory) : VideoSource,
    VideoSink {
    override val contentHint: VideoContentHint = VideoContentHint.Motion

    // Pending frame to render. Serves as a queue with size 1. Synchronized on `pendingFrameLock`.
    // pendingFrameLock also protects the handler (which may be null in `onVideoFrameReceived`
    private var pendingFrame: VideoFrame? = null
    private val pendingFrameLock = Any()

    // Drawers used by this processor
    private val bwDrawer = BlackAndWhiteGlVideoFrameDrawer()
    private val rectDrawer = DefaultGlVideoFrameDrawer()

    // Helper which wraps an OpenGLES texture frame buffer with resize capabilities
    private lateinit var textureFrameBuffer: GlTextureFrameBufferHelper

    // State necessary for EGL operations
    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread("DemoGpuVideoProcessor")
    private var handler: Handler? = null

    // Texture is in use, possibly in another thread
    private var textureInUse = false

    // Dispose has been called and we are waiting on texture to be released
    private var released = false

    // Downstream video sinks
    private val sinks = mutableSetOf<VideoSink>()

    private val DUMMY_PBUFFER_OFFSET = 0

    private val TAG = "DemoGpuVideoProcessor"

    init {
        thread.start()
        handler = Handler(thread.looper)

        handler?.post {
            eglCore = eglCoreFactory.createEglCore()

            // We need to create a dummy surface before we can set the cotext as current
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

            textureFrameBuffer = GlTextureFrameBufferHelper(GLES20.GL_RGBA)

            logger.info(TAG, "Created demo GPU video processor")
        }
    }

    fun release() {
        handler?.post {
            logger.info(TAG, "Releasing GPU video processor source")
            released = true
            // We cannot release until no downstream users have access to texture buffer
            if (!textureInUse) {
                completeRelease()
            }
        }
    }

    override fun addVideoSink(sink: VideoSink) {
        handler?.post {
            sinks.add(sink)
        }
    }

    override fun removeVideoSink(sink: VideoSink) {
        val validHandler = handler ?: return // Already released
        runBlocking(validHandler.asCoroutineDispatcher().immediate) {
            sinks.remove(sink)
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        synchronized(pendingFrameLock) {
            if (pendingFrame != null) {
                // Release pending frame so we don't block any capture source upstream
                pendingFrame?.release()
            }

            if (handler != null) {
                pendingFrame = frame
                pendingFrame?.retain()
                handler?.post(::tryCapturingFrame)
            }
        }
    }

    private fun tryCapturingFrame() {
        check(Looper.myLooper() == handler?.looper)
        // Fetch and render |pendingFrame|.
        var frame: VideoFrame
        synchronized(pendingFrameLock) {
            if (pendingFrame == null) {
                return
            }
            frame = pendingFrame as VideoFrame
            pendingFrame = null
        }

        // Update state
        if (released || textureInUse) {
            frame.release()
            return
        }
        textureInUse = true

        textureFrameBuffer.setSize(frame.getRotatedWidth(), frame.getRotatedHeight())
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.frameBufferId)

        // Convert to black and white
        bwDrawer.drawFrame(frame, 0, 0, frame.getRotatedWidth(), frame.getRotatedHeight(), null)
        // Draw the original frame in the bottom left corner
        rectDrawer.drawFrame(
            frame,
            0,
            0,
            frame.getRotatedWidth() / 2,
            frame.getRotatedHeight() / 2,
            null
        )

        // Must call this otherwise downstream users will not have a synchronized texture
        GLES20.glFinish()
        // Reset to default framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        val processedBuffer =
            VideoFrameTextureBuffer(
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                textureFrameBuffer.textureId,
                null,
                VideoFrameTextureBuffer.Type.TEXTURE_2D,
                Runnable { frameReleased() })
        // Drawer gets rid of any rotation
        val processedFrame = VideoFrame(frame.timestampNs, processedBuffer)

        sinks.forEach { it.onVideoFrameReceived(processedFrame) }
        processedFrame.release()
        frame.release()
    }

    // Called once texture buffer ref count reaches 0
    private fun frameReleased() {
        // Cannot assume this occurs on correct thread
        handler?.post {
            textureInUse = false
            if (released) {
                this.completeRelease()
            } else {
                // May have pending frame
                tryCapturingFrame()
            }
        }
    }

    private fun completeRelease() {
        check(Looper.myLooper() == handler?.looper)

        rectDrawer.release()
        bwDrawer.release()
        textureFrameBuffer.release()
        eglCore.release()

        synchronized(pendingFrameLock) {
            if (pendingFrame != null) {
                pendingFrame?.release()
                pendingFrame = null
            }

            handler?.looper?.quit()
            handler = null
        }
    }
}
