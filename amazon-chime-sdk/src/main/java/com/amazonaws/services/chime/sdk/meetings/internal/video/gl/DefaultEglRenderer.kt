/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultEglRenderer] uses EGL14 to support all functions in [EglRenderer]. It uses a single frame queue to render
 * [VideoFrame] objects passed to it.
 */
class DefaultEglRenderer(private val logger: Logger) : EglRenderer {
    // EGL and GL resources for drawing YUV/OES textures. After initialization, these are only
    // accessed from the render thread. These are reused within init/release cycles.
    private var eglCore: EglCore? = null
    private var surface: Any? = null

    // This handler is protected from onVideoFrameReceived calls during init and release cycles
    // by being synchronized on pendingFrameLock
    private var renderHandler: Handler? = null

    // Cached matrix for draw command
    private val drawMatrix: Matrix = Matrix()

    // Pending frame to render. Serves as a queue with size 1. Synchronized on pendingFrameLock.
    private var pendingFrame: VideoFrame? = null
    private val pendingFrameLock = Any()

    // If true, mirrors the video stream horizontally. Publicly accessible
    override var mirror = false

    // Synchronized on itself, as it may be modified when there renderHandler
    // is or is not running
    override var aspectRatio = 0f
        set(value) {
            synchronized(aspectRatio) {
                logger.info(TAG, "Setting aspect ratio from $field to $value")
                field = value
            }
        }

    private var frameDrawer = DefaultGlVideoFrameDrawer()

    private val TAG = "DefaultEglRenderer"

    override fun init(eglCoreFactory: EglCoreFactory) {
        logger.info(TAG, "Initializing EGL renderer")
        if (renderHandler != null) {
            logger.warn(TAG, "Already initialized")
            return
        }
        val thread = HandlerThread("EglRenderer")
        thread.start()
        this.renderHandler = Handler(thread.looper)

        val validRenderHandler = renderHandler ?: throw UnknownError("No handler in init")
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            eglCore = eglCoreFactory.createEglCore()
            surface?.let {
                logger.info(TAG, "View already has surface, triggering EGL surface creation")
                createEglSurface(it)
            }
        }
    }

    override fun release() {
        logger.info(TAG, "Releasing EGL renderer")
        val validRenderHandler = renderHandler ?: run {
            logger.warn(TAG, "Already released")
            return
        }
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            eglCore?.release()
            eglCore = null
        }

        synchronized(pendingFrameLock) {
            pendingFrame?.release()
            pendingFrame = null

            // Protect this within lock since onVideoFrameReceived can
            // occur from any frame
            validRenderHandler.looper.quitSafely()
            renderHandler = null
        }
    }

    override fun createEglSurface(inputSurface: Any) {
        check(inputSurface is SurfaceTexture || inputSurface is Surface) { "Surface must be SurfaceTexture or Surface" }
        surface = inputSurface
        renderHandler?.post {
            logger.info(TAG, "Request on handler thread to create EGL surface from input surface $surface")
            if (eglCore != null && eglCore?.eglSurface == EGL14.EGL_NO_SURFACE && surface != null) {
                val surfaceAttributess = intArrayOf(EGL14.EGL_NONE)
                eglCore?.eglSurface = EGL14.eglCreateWindowSurface(
                        eglCore?.eglDisplay, eglCore?.eglConfig, surface,
                        surfaceAttributess, 0
                )
                EGL14.eglMakeCurrent(
                        eglCore?.eglDisplay,
                        eglCore?.eglSurface,
                        eglCore?.eglSurface,
                        eglCore?.eglContext
                )

                // Necessary for YUV frames with odd width.
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            }

            // Discard any old frame
            synchronized(pendingFrameLock) {
                pendingFrame?.release()
                pendingFrame = null
            }
        }
    }

    override fun releaseEglSurface() {
        surface = null // Can occur outside of init/release cycle
        val validRenderHandler = this.renderHandler ?: return
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Releasing EGL surface")
            // Release frame drawer while we have a valid current context
            frameDrawer.release()

            EGL14.eglMakeCurrent(
                    eglCore?.eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglCore?.eglDisplay, eglCore?.eglSurface)
            eglCore?.eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        // Set pending frame from this thread and trigger a request to render
        synchronized(pendingFrameLock) {
            // Release any current frame before setting to the latest
            if (pendingFrame != null) {
                pendingFrame?.release()
            }

            if (renderHandler != null) {
                pendingFrame = frame
                pendingFrame?.retain()
                renderHandler?.post(::renderPendingFrame)
            } else {
                logger.warn(TAG, "Skipping frame render request, no render handler thread")
            }
        }
    }

    private fun renderPendingFrame() {
        if (eglCore == null) {
            // May have been called after release
            logger.warn(TAG, "Skipping frame render, no EGL core")
            return
        }

        if (eglCore?.eglSurface == EGL14.EGL_NO_SURFACE) {
            // Verbose since this happens normally when running in background or when view is updating
            logger.verbose(TAG, "Skipping frame render, no EGL surface")
            return
        }

        // Fetch pending frame
        var frame: VideoFrame
        synchronized(pendingFrameLock) {
            if (pendingFrame == null) {
                logger.verbose(TAG, "Skipping frame render, no pending frame to render")
                return
            }
            frame = pendingFrame as VideoFrame
            pendingFrame = null
        }

        // Setup draw matrix transformations
        val frameAspectRatio = frame.getRotatedWidth().toFloat() / frame.getRotatedHeight()
        var drawnAspectRatio = frameAspectRatio
        synchronized(aspectRatio) {
            if (aspectRatio != 0f) {
                drawnAspectRatio = aspectRatio
            }
        }
        val scaleX: Float
        val scaleY: Float
        if (frameAspectRatio > drawnAspectRatio) {
            scaleX = drawnAspectRatio / frameAspectRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = frameAspectRatio / drawnAspectRatio
        }
        drawMatrix.reset()
        drawMatrix.preTranslate(0.5f, 0.5f)
        drawMatrix.preScale(if (mirror) -1f else 1f, 1f)
        drawMatrix.preScale(scaleX, scaleY)
        drawMatrix.preTranslate(-0.5f, -0.5f)

        // Get current surface size so we can set viewport correctly
        val widthArray = IntArray(1)
        EGL14.eglQuerySurface(
                eglCore?.eglDisplay, eglCore?.eglSurface,
                EGL14.EGL_WIDTH, widthArray, 0
        )
        val heightArray = IntArray(1)
        EGL14.eglQuerySurface(
                eglCore?.eglDisplay, eglCore?.eglSurface,
                EGL14.EGL_HEIGHT, heightArray, 0
        )

        // Draw frame and swap buffers, which will make it visible
        frameDrawer.drawFrame(frame, 0, 0, widthArray[0], heightArray[0], drawMatrix)
        EGL14.eglSwapBuffers(eglCore?.eglDisplay, eglCore?.eglSurface)

        frame.release()
    }
}
