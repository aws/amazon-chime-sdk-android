/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.internal.utils.VideoLayoutMeasure
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultEglRenderer
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [SurfaceRenderView] is an implementation of [EglVideoRenderView] which uses EGL14 and OpenGLES2
 * to draw any incoming video buffer types to the surface provided by the inherited [SurfaceView]
 */
open class SurfaceRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback,
        EglVideoRenderView {
    // Accessed only on the main thread.
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var frameRotation = VideoRotation.Rotation0

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // This helper class is used to determine the value to set by setMeasuredDimension in onMeasure
    // (Required for View implementations) which determines the actual size of the view
    private val videoLayoutMeasure: VideoLayoutMeasure = VideoLayoutMeasure()

    private val renderer = DefaultEglRenderer()

    var mirror: Boolean = false
        set(value) {
            renderer.mirror = value
            field = value
        }

    init {
        holder?.addCallback(this)
    }

    override fun init(eglCoreFactory: EglCoreFactory) {
        rotatedFrameWidth = 0
        rotatedFrameHeight = 0

        renderer.init(eglCoreFactory)
    }

    override fun release() {
        renderer.release()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        renderer.releaseEglSurface()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        updateSurfaceSize()

        // Create the EGL surface and set it as current
        holder?.let {
            renderer.createEglSurface(it.surface)
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val size: Point =
                videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        setMeasuredDimension(size.x, size.y)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        renderer.aspectRatio = ((right - left) / (bottom - top).toFloat())
        updateSurfaceSize()
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        // Update internal sizing and layout if frame size changes
        if (rotatedFrameWidth != frame.getRotatedWidth() ||
                rotatedFrameHeight != frame.getRotatedHeight() ||
                frameRotation != frame.rotation
        ) {
            rotatedFrameWidth = frame.getRotatedWidth()
            rotatedFrameHeight = frame.getRotatedHeight()
            frameRotation = frame.rotation

            CoroutineScope(Dispatchers.Main).launch {
                updateSurfaceSize()
                requestLayout()
            }
        }

        renderer.onVideoFrameReceived(frame)
    }

    private fun updateSurfaceSize() {
        if (rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && width != 0 && height != 0) {
            val layoutAspectRatio = width / height.toFloat()
            val frameAspectRatio: Float =
                    rotatedFrameWidth.toFloat() / rotatedFrameHeight
            val drawnFrameWidth: Int
            val drawnFrameHeight: Int
            if (frameAspectRatio > layoutAspectRatio) {
                drawnFrameWidth = ((rotatedFrameHeight * layoutAspectRatio).toInt())
                drawnFrameHeight = rotatedFrameHeight
            } else {
                drawnFrameWidth = rotatedFrameWidth
                drawnFrameHeight = ((rotatedFrameWidth / layoutAspectRatio).toInt())
            }
            // Aspect ratio of the drawn frame and the view is the same.
            val width = min(width, drawnFrameWidth)
            val height = min(height, drawnFrameHeight)

            if (width != surfaceWidth || height != surfaceHeight) {
                surfaceWidth = width
                surfaceHeight = height
                holder.setFixedSize(width, height)
            }
        } else {
            surfaceHeight = 0
            surfaceWidth = surfaceHeight
            holder?.setSizeFromLayout()
        }
    }
}
