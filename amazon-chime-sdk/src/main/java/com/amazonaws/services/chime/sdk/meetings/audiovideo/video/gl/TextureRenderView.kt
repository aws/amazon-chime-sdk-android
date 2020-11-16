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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoScalingType
import com.amazonaws.services.chime.sdk.meetings.internal.utils.VideoLayoutMeasure
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultEglRenderer
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [TextureRenderView] is an implementation of [EglVideoRenderView] which uses EGL14 and OpenGLES2
 * to draw any incoming video buffer types to the surface provided by the inherited [TextureView]
 *
 * Note that since most [TextureRenderView] objects will not be constructed in code, builders must
 * pass in the [Logger] directly before initialization by setting [logger]
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

    private val renderer by lazy {
        // Lazy so we can allow builders to set their own loggers
        DefaultEglRenderer(logger)
    }

    // Public so it can be set, since most users will not be using constructor directly
    var logger: Logger = ConsoleLogger()

    private val TAG = "TextureRenderView"

    /**
     * If true, rendered frames will be mirrored across the vertical axis
     */
    var mirror: Boolean = false
        set(value) {
            logger.debug(TAG, "Setting mirror from $field to $value")
            renderer.mirror = value
            field = value
        }

    /**
     * [VideoScalingType] used to render on this view.  May impact cropping.
     */
    var scalingType: VideoScalingType = VideoScalingType.AspectFill
        set(value) {
            logger.debug(TAG, "Setting scaling type from $field to $value")
            videoLayoutMeasure.scalingType = value
            field = value
        }

    init {
        surfaceTextureListener = this
    }

    override fun init(eglCoreFactory: EglCoreFactory) {
        rotatedFrameWidth = 0
        rotatedFrameHeight = 0

        logger.info(TAG, "Initializing render view")
        renderer.init(eglCoreFactory)
    }

    override fun release() {
        logger.info(TAG, "Releasing render view")
        renderer.release()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val size: Point =
                videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        logger.debug(TAG, "Setting measured dimensions ${size.x}x${size.y}")
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
            logger.info(TAG, "Video frame rotated size changed to ${rotatedFrameWidth}x$rotatedFrameHeight")
            rotatedFrameWidth = frame.getRotatedWidth()
            rotatedFrameHeight = frame.getRotatedHeight()
            frameRotation = frame.rotation

            CoroutineScope(Dispatchers.Main).launch {
                requestLayout()
            }
        }

        renderer.onVideoFrameReceived(frame)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        return
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        return
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        logger.info(TAG, "Surface destroyed, releasing EGL surface")
        renderer.releaseEglSurface()
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        // Create the EGL surface and set it as current
        surface.let {
            logger.info(TAG, "Surface created, creating EGL surface with resource")
            renderer.createEglSurface(it)
        }
    }
}
