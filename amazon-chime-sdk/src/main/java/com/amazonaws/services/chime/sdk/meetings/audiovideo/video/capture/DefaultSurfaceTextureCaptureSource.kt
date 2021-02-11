/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.TimestampAligner
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultSurfaceTextureCaptureSource] will provide a [Surface] which it will listen to
 * and convert to [VideoFrameTextureBuffer] objects
 */
class DefaultSurfaceTextureCaptureSource(
    private val logger: Logger,
    private val width: Int,
    private val height: Int,
    override val contentHint: VideoContentHint = VideoContentHint.None,
    private val eglCoreFactory: EglCoreFactory
) : SurfaceTextureCaptureSource {
    // Publicly accessible
    override lateinit var surface: Surface

    // Additional graphics related state
    private var textureId: Int = 0
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var eglCore: EglCore

    // EGLContext should be valid on this thread
    private val thread: HandlerThread = HandlerThread("DefaultSurfaceTextureCaptureSource")
    private val handler: Handler

    // This class helps align timestamps from the surface to timestamps
    // originating from different monotonic clocks in native code
    private val timestampAligner = TimestampAligner()

    // Frame available listener was called when a texture was already in use
    // SurfaceTextures must be updated by calling `updateTexImage` (usually in
    // OnFrameAvailableListener) however if something downstream is holding onto
    // a reference, then calling `updateTexImage` would invalidate their texture.
    // We use the release callback to trigger `updateTexImage` and the capture of
    // a new frame if that is the case.
    private var pendingAvailableFrame = false

    // Texture is in use, possibly in another thread. See comment above for why
    // we only allow one texture in flight at a time (i.e. because the SurfaceTexture owns
    // it, and can only own one texture at a time.
    private var textureBufferInFlight = false

    // Dispose has been called and we are waiting on texture to be released before we deallocate
    // non-GC-able resources
    private var releasePending = false

    // We will maintain close to the min FPS by simply posting a request to resend
    // a previously sent frame delayed by (1f/minFps)
    override var minFps = 0
    // Buffer the delay so it doesn't trigger on small framerate variations (will cause
    // min FPS to be slightly inaccurate but shouldn't have a huge impact given its purpose)
    private val RESEND_DELAY_BUFFER_MS = 10
    // Use this to check from the delayed request whether we have recently
    // enough sent a new frame
    private var lastAlignedTimestamp: Long? = null

    // Concurrency modification could happen when sink gets
    // added/removed from another thread while sending frames
    private var sinks = ConcurrentSet.createConcurrentSet<VideoSink>()

    private val TAG = "SurfaceTextureCaptureSource"

    private val DUMMY_PBUFFER_OFFSET = 0

    init {
        thread.start()
        handler = Handler(thread.looper)

        runBlocking(handler.asCoroutineDispatcher().immediate) {
            eglCore = eglCoreFactory.createEglCore()

            val surfaceAttributes =
                intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE)
            eglCore.eglSurface = EGL14.eglCreatePbufferSurface(
                eglCore.eglDisplay,
                eglCore.eglConfig,
                surfaceAttributes,
                DUMMY_PBUFFER_OFFSET
            )
            EGL14.eglMakeCurrent(
                eglCore.eglDisplay,
                eglCore.eglSurface,
                eglCore.eglSurface,
                eglCore.eglContext
            )
            GlUtil.checkGlError("Failed to set dummy surface to initialize surface texture video source")

            textureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture.setDefaultBufferSize(width, height)
            @SuppressLint("Recycle")
            surface = Surface(surfaceTexture)

            logger.info(
                TAG,
                "Created surface texture for video source with dimensions $width x $height"
            )
        }
    }

    override fun start() {
        handler.post {
            surfaceTexture.setOnFrameAvailableListener({
                pendingAvailableFrame = true
                tryCapturingFrame()
            }, handler)
        }
    }

    override fun stop() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Setting on frame available listener to null")
            surfaceTexture.setOnFrameAvailableListener(null)
        }
    }

    // Unused
    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {}
    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {}

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun release() {
        handler.post {
            logger.info(TAG, "Releasing surface texture capture source")
            if (!textureBufferInFlight) {
                completeRelease()
            } else {
                // If we have a frame in flight we cannot immediately release
                // This will cause onFrameRelease to trigger completeRelease
                releasePending = true
            }
        }
    }

    private fun tryCapturingFrame() {
        // Check to see if we are in valid state for capturing a new frame
        //  * Don't capture a new frame if we are in the process of releasing
        //  * Don't capture a new frame if we don't have one pending
        //  * Don't capture a new frame if there is a buffer in flight as to not invalidate it
        if (releasePending || !pendingAvailableFrame || textureBufferInFlight) {
            return
        }

        textureBufferInFlight = true
        pendingAvailableFrame = false

        // This call is what actually updates the texture
        surfaceTexture.updateTexImage()

        val transformMatrix = FloatArray(16)
        surfaceTexture.getTransformMatrix(transformMatrix)

        val buffer =
            VideoFrameTextureBuffer(
                width,
                height,
                textureId,
                GlUtil.convertToMatrix(transformMatrix),
                VideoFrameTextureBuffer.Type.TEXTURE_OES,
                Runnable { onFrameReleased() })
        val alignedTimestamp = timestampAligner.translateTimestamp(surfaceTexture.timestamp)
        val frame = VideoFrame(alignedTimestamp, buffer)

        sinks.forEach { it.onVideoFrameReceived(frame) }
        frame.release()

        // If we have min FPS set, check in a while if we should resend the previous frame
        if (minFps > 0) {
            lastAlignedTimestamp = alignedTimestamp
            val resendDelayMs = RESEND_DELAY_BUFFER_MS + (1f / minFps) * 1000
            handler.postDelayed({
                if (alignedTimestamp == lastAlignedTimestamp) {
                    pendingAvailableFrame = true
                    tryCapturingFrame()
                }
            }, resendDelayMs.toLong())
        }
    }

    private fun onFrameReleased() {
        // Cannot assume this occurs on correct thread
        handler.post {
            textureBufferInFlight = false
            if (releasePending) {
                completeRelease()
            } else {
                // May have pending frame which was waiting on the previous
                // frame to no longer have users so that that texture can be
                // reused
                tryCapturingFrame()
            }
        }
    }

    private fun completeRelease() {
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        surfaceTexture.release()
        surface.release()

        eglCore.release()

        timestampAligner.dispose()
        logger.info(TAG, "Finished releasing surface texture capture source")

        handler.looper.quit()
    }
}
