/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import android.graphics.Matrix
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.xodee.client.video.YuvUtil
import java.nio.ByteBuffer
import java.security.InvalidParameterException
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [DefaultGlVideoFrameDrawer] simply draws the frames as opaque quads onto the current surface.
 * This drawer supports all buffer types in the SDK.
 */
class DefaultGlVideoFrameDrawer() : GlVideoFrameDrawer {
    /**
     * Helper class for uploading YUV bytebuffer frames to textures that handles stride > width. This
     * class keeps an internal ByteBuffer to avoid unnecessary allocations for intermediate copies.
     * Lazily initialized and can be reused after release
     */
    private class I420BufferTextureUploader {
        // Intermediate copy buffer for uploading yuv frames that are not packed, i.e. stride > width.
        private var copyBuffer: ByteBuffer? = null

        // Output texture IDs
        var yuvTextures: IntArray? = null

        fun uploadFromBuffer(buffer: VideoFrameI420Buffer): IntArray {
            val strides = intArrayOf(buffer.strideY, buffer.strideU, buffer.strideV)
            val planes = arrayOf(buffer.dataY, buffer.dataU, buffer.dataV)
            return uploadYuvData(buffer.width, buffer.height, strides, planes)
        }

        fun release() {
            copyBuffer = null
            yuvTextures?.let { GLES20.glDeleteTextures(3, it, 0) }
        }

        private fun uploadYuvData(
            width: Int,
            height: Int,
            strides: IntArray,
            planes: Array<ByteBuffer>
        ): IntArray {
            val planeWidths = intArrayOf(width, width / 2, width / 2)
            val planeHeights = intArrayOf(height, height / 2, height / 2)
            // Make a first pass to see if we need a temporary copy buffer.
            var copyCapacityNeeded = 0
            for (i in 0..2) {
                if (strides[i] > planeWidths[i]) {
                    copyCapacityNeeded = max(copyCapacityNeeded, planeWidths[i] * planeHeights[i])
                }
            }
            // Allocate copy buffer if necessary.
            if (copyBuffer?.capacity() ?: 0 < copyCapacityNeeded) {
                copyBuffer = ByteBuffer.allocateDirect(copyCapacityNeeded)
            }

            // Make sure YUV textures are allocated.
            val validYuvTextures: IntArray = yuvTextures ?: run {
                yuvTextures = IntArray(3).also {
                    for (i in 0..2) {
                        it[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D)
                    }
                }
                return@run yuvTextures as IntArray
            }

            // Upload each plane.
            for (i in 0..2) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, validYuvTextures[i])

                // GLES only accepts packed data, i.e. stride == planeWidth.
                val packedByteBuffer: ByteBuffer = if (strides[i] == planeWidths[i]) {
                    planes[i] // Input is packed already.
                } else {
                    YuvUtil.copyPlane(
                        planes[i], strides[i], copyBuffer,
                        planeWidths[i], planeWidths[i], planeHeights[i]
                    )
                    copyBuffer ?: return IntArray(0)
                }

                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    planeWidths[i], planeHeights[i], 0,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    packedByteBuffer
                )
            }
            return validYuvTextures
        }
    }

    /**
     * Helper class for uploading RGBA bytebuffer frames to textures that handles stride > width. This
     * class keeps an internal ByteBuffer to avoid unnecessary allocations for intermediate copies.
     * Lazily initialized and can be reused after release
     */
    private class RGBABufferTextureUploader {
        // Intermediate copy buffer for uploading yuv frames that are not packed, i.e. stride > width.
        private var copyBuffer: ByteBuffer? = null

        // Output texture IDs
        var textureId: Int = 0

        fun uploadFromBuffer(buffer: VideoFrameRGBABuffer): Int {
            return uploadRgbaData(buffer.width, buffer.height, buffer.data, buffer.stride)
        }

        fun release() {
            copyBuffer = null
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }

        private fun uploadRgbaData(
            width: Int,
            height: Int,
            data: ByteBuffer,
            stride: Int
        ): Int {
            // Make a first pass to see if we need a temporary copy buffer.
            var copyCapacityNeeded = 0
            if (stride > width * 4) {
                copyCapacityNeeded = max(copyCapacityNeeded, width * 4 * height)
            }
            // Allocate copy buffer if necessary.
            if (copyBuffer?.capacity() ?: 0 < copyCapacityNeeded) {
                copyBuffer = ByteBuffer.allocateDirect(copyCapacityNeeded)
            }

            // Make sure YUV textures are allocated.
            val textureId =
                if (textureId == 0) GlUtil.generateTexture(GLES20.GL_TEXTURE_2D) else textureId

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            // GLES only accepts packed data, i.e. stride == planeWidth.
            val packedByteBuffer: ByteBuffer = if (stride == width * 4) {
                data // Input is packed already.
            } else {
                YuvUtil.copyPlane(
                    data, stride,
                    copyBuffer, width * 4,
                    width, height
                )
                copyBuffer ?: return 0
            }

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                packedByteBuffer
            )
            return textureId
        }
    }

    // We will switch shaders on the fly to support all buffer types
    private var currentShaderType: ShaderType? = null

    // Shader metadata
    private var program: Int = -1
    private var vertexPositionLocation = 0
    private var textureCoordinateLocation = 0
    private var textureMatrixLocation = 0

    // Variables to store post-transform render information
    private val renderMatrix = Matrix()
    private val renderDestinationPoints = FloatArray(6)
    private var renderWidth = 0
    private var renderHeight = 0

    private var i420Uploader: I420BufferTextureUploader? = I420BufferTextureUploader()
    private var rgbaUploader: RGBABufferTextureUploader? = RGBABufferTextureUploader()

    override fun drawFrame(
        frame: VideoFrame,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        additionalRenderMatrix: Matrix?
    ) {
        val isTextureFrame = frame.buffer is VideoFrameTextureBuffer

        // Set up and account for transformation
        val width: Int = frame.getRotatedWidth()
        val height: Int = frame.getRotatedHeight()
        calculateTransformedRenderSize(width, height, additionalRenderMatrix)
        if (renderWidth <= 0 || renderHeight <= 0) {
            return
        }
        renderMatrix.reset()
        // Perform mirror and rotation around (0.5, 0.5) since that is the center of the texture.
        renderMatrix.preTranslate(0.5f, 0.5f)
        if (frame.buffer is VideoFrameI420Buffer) {
            renderMatrix.preScale(1f, -1f) // I420-frames are upside down
        }
        renderMatrix.preRotate(frame.rotation.degrees.toFloat())
        renderMatrix.preTranslate(-0.5f, -0.5f)
        if (additionalRenderMatrix != null) {
            renderMatrix.preConcat(additionalRenderMatrix)
        }

        // Render supported frames
        if (isTextureFrame) {
            val textureBuffer = frame.buffer as VideoFrameTextureBuffer
            val finalMatrix = Matrix(textureBuffer.transformMatrix)
            finalMatrix.preConcat(renderMatrix)
            val finalGlMatrix = GlUtil.convertToGlTransformMatrix(finalMatrix)
            when (textureBuffer.type) {
                VideoFrameTextureBuffer.Type.TEXTURE_OES -> drawTextureOes(
                    textureBuffer.textureId, finalGlMatrix,
                    viewportX, viewportY,
                    viewportWidth, viewportHeight
                )
                VideoFrameTextureBuffer.Type.TEXTURE_2D -> drawTexture2d(
                    textureBuffer.textureId, finalGlMatrix,
                    viewportX, viewportY,
                    viewportWidth, viewportHeight
                )
            }
        } else if (frame.buffer is VideoFrameI420Buffer) {
            if (i420Uploader == null) {
                i420Uploader = I420BufferTextureUploader()
            }

            i420Uploader?.let {
                it.uploadFromBuffer(frame.buffer)
                drawYuv(
                    it.yuvTextures,
                    GlUtil.convertToGlTransformMatrix(renderMatrix),
                    viewportX, viewportY, viewportWidth, viewportHeight
                )
            }
        } else if (frame.buffer is VideoFrameRGBABuffer) {
            val textureId = rgbaUploader?.uploadFromBuffer(frame.buffer)
            if (textureId != null) {
                drawTexture2d(
                    textureId,
                    GlUtil.convertToGlTransformMatrix(renderMatrix),
                    viewportX,
                    viewportY,
                    viewportWidth,
                    viewportHeight
                )
            }
        }
    }

    // Calculate the frame size after |renderMatrix| is applied. Stores the output in member variables
    // |renderWidth| and |renderHeight| to avoid allocations since this function is called for every frame.
    private fun calculateTransformedRenderSize(
        frameWidth: Int,
        frameHeight: Int,
        renderMatrix: Matrix?
    ) {
        if (renderMatrix == null) {
            renderWidth = frameWidth
            renderHeight = frameHeight
            return
        }
        // Transform the texture coordinates (in the range [0, 1]) according to |renderMatrix|.
        renderMatrix.mapPoints(renderDestinationPoints, srcPoints)

        // Multiply with the width and height to get the positions in terms of pixels.
        for (i in 0..2) {
            renderDestinationPoints[i * 2 + 0] = renderDestinationPoints[i * 2 + 0] * frameWidth
            renderDestinationPoints[i * 2 + 1] = renderDestinationPoints[i * 2 + 1] * frameHeight
        }

        fun distance(x0: Float, y0: Float, x1: Float, y1: Float): Int {
            return hypot(x1 - x0.toDouble(), y1 - y0.toDouble()).roundToInt()
        }
        // Get the length of the sides of the transformed rectangle in terms of pixels.
        renderWidth = distance(
            renderDestinationPoints[0],
            renderDestinationPoints[1],
            renderDestinationPoints[2],
            renderDestinationPoints[3]
        )
        renderHeight = distance(
            renderDestinationPoints[0],
            renderDestinationPoints[1],
            renderDestinationPoints[4],
            renderDestinationPoints[5]
        )
    }

    private fun drawTextureOes(
        oesTextureId: Int,
        texMatrix: FloatArray?,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ) {
        prepareShader(ShaderType.TEXTURE_OES, texMatrix)

        // Bind the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        // Draw the texture.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("Failed to draw GL_TEXTURE_EXTERNAL_OES texture")

        // Reset texture back to default texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun drawTexture2d(
        textureId: Int,
        texMatrix: FloatArray?,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ) {
        prepareShader(ShaderType.TEXTURE_2D, texMatrix)

        // Bind the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Draw the texture.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("Failed to draw GL_TEXTURE_2D texture")

        // Unbind the texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun drawYuv(
        yuvTextures: IntArray?,
        texMatrix: FloatArray?,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ) {
        prepareShader(ShaderType.YUV, texMatrix)

        // Bind the textures.
        for (i in 0..2) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures!![i])
        }
        // Draw the textures.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("Failed to draw YUV textures")

        // Unbind the textures
        for (i in 0..2) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    private fun prepareShader(shaderType: ShaderType, texMatrix: FloatArray?) {
        if (currentShaderType != shaderType) {
            // Allocate new shader.
            var fragmentShader: String
            currentShaderType = shaderType.also {
                fragmentShader = when (shaderType) {
                    ShaderType.TEXTURE_OES -> FRAGMENT_SHADER_OES
                    ShaderType.TEXTURE_2D -> FRAGMENT_SHADER_RGB
                    ShaderType.YUV -> FRAGMENT_SHADER_YUV
                }
            }
            if (program != -1) {
                GLES20.glDeleteProgram(program)
            }
            program = GlUtil.createProgram(VERTEX_SHADER, fragmentShader)
            GLES20.glUseProgram(program)
            GlUtil.checkGlError("Failed to create program")

            // Set input texture units.
            if (shaderType == ShaderType.YUV) {
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program, INPUT_TEXTURE_Y_NAME), 0)
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program, INPUT_TEXTURE_U_NAME), 1)
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program, INPUT_TEXTURE_V_NAME), 2)
            } else {
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program, INPUT_TEXTURE_NAME), 0)
            }
            GlUtil.checkGlError("Failed to setup program texture inputs")

            // Get input locations
            textureMatrixLocation = GLES20.glGetUniformLocation(program, TEXTURE_MATRIX_NAME)
            vertexPositionLocation =
                GLES20.glGetAttribLocation(program, INPUT_VERTEX_COORDINATE_NAME)
            textureCoordinateLocation =
                GLES20.glGetAttribLocation(program, INPUT_TEXTURE_COORDINATE_NAME)
            if (textureMatrixLocation == -1 || vertexPositionLocation == -1 || textureCoordinateLocation == -1) {
                throw InvalidParameterException("Failed to get shader locations")
            }
        }
        GLES20.glUseProgram(program)
        GlUtil.checkGlError("Failed to use program")

        // Upload the vertex coordinates.
        GLES20.glEnableVertexAttribArray(vertexPositionLocation)
        GLES20.glVertexAttribPointer(
            vertexPositionLocation, 2, GLES20.GL_FLOAT, false, 0, GlUtil.FULL_RECTANGLE_VERTEX_COORDINATES
        )

        // Upload the texture coordinates.
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
        GLES20.glVertexAttribPointer(
            textureCoordinateLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            GlUtil.FULL_RECTANGLE_TEXTURE_COORDINATES
        )

        // Upload the texture transformation matrix.
        GLES20.glUniformMatrix4fv(textureMatrixLocation, 1, false, texMatrix, 0)
        GlUtil.checkGlError("Failed to upload shader inputs")
    }

    override fun release() {
        i420Uploader?.release()
        i420Uploader = null

        rgbaUploader?.release()
        rgbaUploader = null

        currentShaderType = null
        if (program != -1) {
            GLES20.glUseProgram(0)
            GLES20.glDeleteProgram(program)
            program = -1
        }
    }

    companion object {
        // These points are used to calculate the size of the part of the frame we are rendering.
        private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

        private enum class ShaderType { TEXTURE_OES, TEXTURE_2D, YUV }

        private val INPUT_VERTEX_COORDINATE_NAME = "aPosition"
        private val INPUT_TEXTURE_COORDINATE_NAME = "aTextureCoordinate"
        private val TEXTURE_MATRIX_NAME = "uTextureMatrix"
        private val VERTEX_SHADER =
            """
        varying vec2 vTextureCoordinate;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoordinate;
        uniform mat4 uTextureMatrix;
        void main() {
            gl_Position = aPosition;
            vTextureCoordinate = (uTextureMatrix * aTextureCoordinate).xy;
        }
        """

        private val INPUT_TEXTURE_NAME = "sTexture"
        private val FRAGMENT_SHADER_OES =
            """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoordinate;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoordinate);
        }
        """

        private val FRAGMENT_SHADER_RGB =
            """
        precision mediump float;
        varying vec2 vTextureCoordinate;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoordinate);
        }
        """

        private val INPUT_TEXTURE_Y_NAME = "sTextureY"
        private val INPUT_TEXTURE_U_NAME = "sTextureU"
        private val INPUT_TEXTURE_V_NAME = "sTextureV"

        private val FRAGMENT_SHADER_YUV =
            """
        precision mediump float;
        varying vec2 vTextureCoordinate;
        uniform sampler2D sTextureY;
        uniform sampler2D sTextureU;
        uniform sampler2D sTextureV;
        vec4 sample(vec2 p) {
            float y = texture2D(sTextureY, p).r * 1.16438;
            float u = texture2D(sTextureU, p).r;
            float v = texture2D(sTextureV, p).r;
            return vec4(y + 1.59603 * v - 0.874202,
                        y - 0.391762 * u - 0.812968 * v + 0.531668,
                        y + 2.01723 * u - 1.08563, 1);
        }
        void main() {
            gl_FragColor = sample(vTextureCoordinate);
        }
        """
    }
}
