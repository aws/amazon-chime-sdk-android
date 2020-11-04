/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.graphics.Matrix
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.internal.video.gl.GlVideoFrameDrawer
import java.security.InvalidParameterException
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * [BlackAndWhiteGlVideoFrameDrawer] simply draws the frames as opaque quads onto the current surface with a black and white filter.
 * It only supports OES texture frames.
 */
class BlackAndWhiteGlVideoFrameDrawer : GlVideoFrameDrawer {
    private var program: Int = -1
    private var vertexPositionLocation = 0
    private var textureCoordinateLocation = 0
    private var textureMatrixLocation = 0

    // Variables to store post-transform render information
    private val renderMatrix = Matrix()
    private val renderDestinationPoints = FloatArray(6)
    private var renderWidth = 0
    private var renderHeight = 0

    override fun drawFrame(
        frame: VideoFrame,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        additionalRenderMatrix: Matrix?
    ) {
        val isTextureFrame = frame.buffer is VideoFrameTextureBuffer
        val textureBuffer = frame.buffer as VideoFrameTextureBuffer
        if (!isTextureFrame || textureBuffer.type != VideoFrameTextureBuffer.Type.TEXTURE_OES) {
            throw InvalidParameterException("Only OES texture frames are expected")
        }

        // Set up and account for transformation
        val width: Int = frame.getRotatedWidth()
        val height: Int = frame.getRotatedHeight()
        calculateTransformedRenderSize(width, height, additionalRenderMatrix)
        if (renderWidth <= 0 || renderHeight <= 0) {
            return
        }
        renderMatrix.reset()
        // Need to translate origin before rotating
        renderMatrix.preTranslate(0.5f, 0.5f)
        renderMatrix.preRotate(frame.rotation.degrees.toFloat())
        // Translate back
        renderMatrix.preTranslate(-0.5f, -0.5f)
        // Apply additional matrix
        additionalRenderMatrix?.let { renderMatrix.preConcat(it) }

        // Apply texture frame matrix
        val finalMatrix = Matrix(textureBuffer.transformMatrix)
        finalMatrix.preConcat(renderMatrix)
        val finalGlMatrix = GlUtil.convertToGlTransformMatrix(finalMatrix)

        prepareShader(finalGlMatrix)

        // Bind the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureBuffer.textureId)

        // Draw the texture.
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("Failed to draw OES texture")

        // Reset texture back to default texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    // Calculate the frame size after |renderMatrix| is applied. Stores the output in member variables
    // |renderWidth| and |renderHeight| to avoid allocations since this function is called for every frame.
    private fun calculateTransformedRenderSize(frameWidth: Int, frameHeight: Int, renderMatrix: Matrix?) {
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

    private fun prepareShader(texMatrix: FloatArray) {
        if (program == -1) {
            // Allocate new shader.
            program = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_OES)
            GLES20.glUseProgram(program)
            GlUtil.checkGlError("Failed to create program")

            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, INPUT_TEXTURE_NAME), 0)
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
            vertexPositionLocation, 2, GLES20.GL_FLOAT,
            false, 0, GlUtil.FULL_RECTANGLE_VERTEX_COORDINATES
        )

        // Upload the texture coordinates.
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
        GLES20.glVertexAttribPointer(
            textureCoordinateLocation, 2, GLES20.GL_FLOAT,
            false, 0, GlUtil.FULL_RECTANGLE_TEXTURE_COORDINATES
        )

        // Upload the texture transformation matrix.
        GLES20.glUniformMatrix4fv(textureMatrixLocation, 1, false, texMatrix, 0)
        GlUtil.checkGlError("Failed to upload shader inputs")
    }

    override fun release() {
        if (program != -1) {
            GLES20.glUseProgram(0)
            GLES20.glDeleteProgram(program)
            program = -1
        }
    }

    companion object {
        // These points are used to calculate the size of the part of the frame we are rendering.
        private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

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
            vec4 Color = texture2D(sTexture, vTextureCoordinate);
            gl_FragColor = vec4(vec3(Color.r + Color.g + Color.b) / 3.0, Color.a);
        }
        """
    }
}
