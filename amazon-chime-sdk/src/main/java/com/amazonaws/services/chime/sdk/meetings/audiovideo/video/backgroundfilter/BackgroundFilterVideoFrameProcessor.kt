/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.opengl.GLES20
import com.amazonaws.services.chime.cwt.ModelState
import com.amazonaws.services.chime.cwt.PredictResult
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.DefaultGlVideoFrameDrawer
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.GlTextureFrameBufferHelper
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.JniUtil
import java.nio.ByteBuffer

/**
 * [BackgroundFilterVideoFrameProcessor] Draws frames to RGBA, converts to CPU, identifies the
 * foreground person and applies filter (blur or replacement) to a video frame.
 * @param logger: [Logger] - Logger to log the data.
 * @param context: [Context] - Context to create blur and segmentation processor.
 * @param tag: [String] - Tag for logger.
 */
class BackgroundFilterVideoFrameProcessor(
    private val logger: Logger,
    context: Context,
    tag: String
) {
    // The camera capture source currently output OES texture frames, so we draw them to a frame buffer that
    // we can read to host memory from.
    private val rectDrawer = DefaultGlVideoFrameDrawer()

    // Helper which wraps an OpenGLES texture frame buffer with resize capabilities.
    private val textureFrameBuffer = GlTextureFrameBufferHelper(GLES20.GL_RGBA)

    private val TAG = tag

    private val segmentationProcessor = SegmentationProcessor(context)
    private var modelStateMsg: String? = null
    private var predictMsg: String? = null

    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0

    private val defaultInputModelShape = ModelShape()
    private val channels = defaultInputModelShape.channels
    private val bitmapConfig = Bitmap.Config.ARGB_8888

    fun getInputBitmap(frame: VideoFrame): Bitmap {
        val rgbaData = this.getByteBufferFromInputVideoFrame(frame)
        val inputBitmap = Bitmap.createBitmap(
            frame.getRotatedWidth(),
            frame.getRotatedHeight(),
            bitmapConfig
        )
        inputBitmap.copyPixelsFromBuffer(rgbaData)
        JniUtil.nativeFreeByteBuffer(rgbaData)
        return inputBitmap
    }

    fun getByteBufferFromInputVideoFrame(frame: VideoFrame): ByteBuffer {
        // Note: This processor assumes that the incoming call will be on a valid EGL context.
        textureFrameBuffer.setSize(frame.getRotatedWidth(), frame.getRotatedHeight())
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.frameBufferId)

        val matrix = Matrix()
        matrix.preTranslate(0.5f, 0.5f)
        matrix.preScale(1f, -1f)
        matrix.preTranslate(-0.5f, -0.5f)

        // Note the draw call will account for any rotation, so we need to account for that in viewport width/height.
        rectDrawer.drawFrame(
            frame,
            0,
            0,
            frame.getRotatedWidth(),
            frame.getRotatedHeight(),
            matrix
        )

        // Read RGBA data to native byte buffer.
        val rgbaData =
            JniUtil.nativeAllocateByteBuffer(frame.getRotatedWidth() * frame.getRotatedHeight() * channels)
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
        return rgbaData
    }

    /**
     * Scales the input bitmap as per ML model specification.
     */
    fun getScaledInputBitmap(frame: VideoFrame, inputBitmap: Bitmap): Bitmap {
        var scaledHeight = defaultInputModelShape.height
        var scaledWidth = defaultInputModelShape.width
        // Swap height and width for landscape orientation.
        if (frame.rotation.degrees % 180 == 0) {
            scaledHeight = defaultInputModelShape.width
            scaledWidth = defaultInputModelShape.height
        }
        return Bitmap.createScaledBitmap(inputBitmap, scaledWidth, scaledHeight, false)
    }

    fun getProcessedFrame(
        frame: VideoFrame,
        filteredBitmap: Bitmap?,
        rgbaData: ByteBuffer
    ): VideoFrame {
        var filteredByteBuffer: ByteBuffer
        if (filteredBitmap == null) {
            // Display original frame when there is an error getting segmentation mask and/or blurring.
            filteredByteBuffer = rgbaData
        } else {
            filteredByteBuffer =
                JniUtil.nativeAllocateByteBuffer(frame.getRotatedWidth() * frame.getRotatedHeight() * channels)
            filteredBitmap.copyPixelsToBuffer(filteredByteBuffer)
            filteredByteBuffer.position(0)
        }
        val rgbaBuffer =
            VideoFrameRGBABuffer(
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                filteredByteBuffer,
                frame.getRotatedWidth() * channels,
                Runnable { JniUtil.nativeFreeByteBuffer(filteredByteBuffer) })
        return VideoFrame(frame.timestampNs, rgbaBuffer)
    }

    fun getSegmentationMask(
        scaledInputBitmap: Bitmap
    ): Bitmap? {
        // Load model only when previous frame dimensions does not match new frame dimensions.
        if (scaledInputBitmap.width != cachedWidth || scaledInputBitmap.height != cachedHeight) {
            cachedWidth = scaledInputBitmap.width
            cachedHeight = scaledInputBitmap.height
            // Load model.
            segmentationProcessor.initialize(
                scaledInputBitmap.width,
                scaledInputBitmap.height,
                defaultInputModelShape
            )
            val modelState = segmentationProcessor.modelState
            // Logs the error or info message only the first time model is loaded to avoid exploding logs for every frame.
            if (modelState != ModelState.LOADING && modelState != ModelState.LOADED && modelStateMsg == null) {
                modelStateMsg = "Failed to load model with model state $modelState"
                modelStateMsg?.let {
                    logger.error(TAG, it)
                }
            } else {
                if (modelStateMsg == null) {
                    modelStateMsg = "Model State $modelState"
                    modelStateMsg?.let {
                        logger.info(TAG, it)
                    }
                }
            }
        }

        // Set input for model.
        val inputBuffer: ByteBuffer = segmentationProcessor.getInputBuffer()
        scaledInputBitmap.copyPixelsToBuffer(inputBuffer)

        // Predict the result of model.
        val predictResult = segmentationProcessor.predict()
        // Logs the error message only the first time predict is called to avoid exploding logs for every frame.
        if (predictResult == PredictResult.ERROR && predictMsg == null) {
            predictMsg = "Error segmenting background from foreground"
            predictMsg?.let {
                logger.error(TAG, it)
            }
        }

        // Set output after prediction.
        val outputBuffer: ByteBuffer = segmentationProcessor.getOutputBuffer()
        val outputBitmap =
            Bitmap.createBitmap(scaledInputBitmap.width, scaledInputBitmap.height, bitmapConfig)
        outputBitmap.copyPixelsFromBuffer(outputBuffer)
        return outputBitmap
    }

    fun drawImageWithMask(
        scaledInputBitmap: Bitmap,
        outputBitmap: Bitmap?,
        filteredBitmap: Bitmap?
    ): Bitmap? {
        val paint = Paint()
        var paintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
        paint.xfermode = PorterDuffXfermode(paintMode)
        val canvas = outputBitmap?.let { Canvas(it) }
        // Draw mask.
        canvas?.drawBitmap(scaledInputBitmap, 0.0f, 0.0f, paint)
        paintMode = PorterDuff.Mode.DST_OVER
        paint.xfermode = PorterDuffXfermode(paintMode)
        if (filteredBitmap != null) {
            canvas?.drawBitmap(filteredBitmap, 0.0f, 0.0f, paint)
        }
        return outputBitmap
    }

    fun release() {
        rectDrawer.release()
        textureFrameBuffer.release()
    }
}
