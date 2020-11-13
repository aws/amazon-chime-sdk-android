/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.content.Context
import android.graphics.Point
import android.opengl.EGL14
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoScalingType
import com.amazonaws.services.chime.sdk.meetings.internal.utils.VideoLayoutMeasure
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultEglRenderer
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [SurfaceRenderView] is an implementation of [EglVideoRenderView] which uses EGL14 and OpenGLES2
 * to draw any incoming video buffer types to the surface provided by the inherited [SurfaceView].
 *
 * Note that since most [SurfaceRenderView] objects will not be constructed in code, builders must
 * pass in the [Logger] directly before initialization by setting [logger]
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

    // Lazy so we can allow builders to set their own loggers using self.logger
    // If no logger is passed in we will fallback on ConsoleLogger
    private val renderer by lazy {
        DefaultEglRenderer(logger)
    }

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

    /**
     * Enables fixed size for the surface. This provides better performance but might be buggy on some
     * devices, for example on Galaxy S9, [EGL14.eglQuerySurface] will not return update values until
     * after the first [EGL14.eglSwapBuffers] call, and will lead to cropping or black boxing on resolution
     * changes. By default this is turned off.
     */
    var hardwareScaling: Boolean = false
        set(value) {
            logger.debug(TAG, "Setting hardware scaling from $field to $value")
            field = value
        }

    // Public so it can be set, since most users will not be using constructor directly
    var logger: Logger = ConsoleLogger()

    private val TAG = "SurfaceRenderView"

    init {
        holder?.addCallback(this)
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

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        logger.info(TAG, "Surface destroyed, releasing EGL surface")
        renderer.releaseEglSurface()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        updateSurfaceSize()

        // Create the EGL surface and set it as current
        holder?.let {
            logger.info(TAG, "Surface created, creating EGL surface with resource")
            renderer.createEglSurface(it.surface)
        }
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

            logger.info(TAG, "Video frame rotated size changed to ${rotatedFrameWidth}x$rotatedFrameHeight with rotation $frameRotation")

            CoroutineScope(Dispatchers.Main).launch {
                updateSurfaceSize()
                requestLayout()
            }
        }
        renderer.onVideoFrameReceived(frame)
    }

    private fun updateSurfaceSize() {
        if (hardwareScaling && rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && width != 0 && height != 0) {
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

            logger.info(TAG, "Updating surface size frame size: ${rotatedFrameWidth}x$rotatedFrameHeight, " +
                    "requested surface size: ${width}x$height, old surface size: ${surfaceWidth}x$surfaceHeight")
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
