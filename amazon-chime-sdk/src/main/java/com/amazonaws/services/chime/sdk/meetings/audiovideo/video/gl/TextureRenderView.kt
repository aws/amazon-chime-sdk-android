/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.content.Context
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.internal.utils.VideoLayoutMeasure
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultEglRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [TextureRenderView] is an implementation of [EglVideoRenderView] which uses EGL14 and OpenGLES2
 * to draw any incoming video buffer types to the surface provided by the inherited [TextureView]
 */
open class TextureRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle), TextureView.SurfaceTextureListener,
        EglVideoRenderView {
    // Accessed only on the main thread.
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var frameRotation = VideoRotation.Rotation0

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
        surfaceTextureListener = this
    }

    override fun init(eglCoreFactory: EglCoreFactory) {
        rotatedFrameWidth = 0
        rotatedFrameHeight = 0

        renderer.init(eglCoreFactory)
    }

    override fun release() {
        renderer.release()
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
                requestLayout()
            }
        }

        renderer.onVideoFrameReceived(frame)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        return
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        return
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        renderer.releaseEglSurface()
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        // Create the EGL surface and set it as current
        surface?.let {
            renderer.createEglSurface(it)
        }
    }
}
