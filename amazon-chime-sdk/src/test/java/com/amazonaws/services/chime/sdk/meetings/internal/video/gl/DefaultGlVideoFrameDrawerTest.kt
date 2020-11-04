/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.xodee.client.video.YuvUtil
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.nio.ByteBuffer
import kotlinx.coroutines.android.HandlerDispatcher
import org.junit.Before
import org.junit.Test

class DefaultGlVideoFrameDrawerTest {

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    private lateinit var mockHandlerDispatcher: HandlerDispatcher

    @MockK(relaxed = true)
    private lateinit var mockEglCore: EglCore

    private lateinit var testGlVideoFrameDrawer: DefaultGlVideoFrameDrawer

    private lateinit var mockLooper: Looper

    @MockK(relaxed = true)
    private lateinit var mockMatrix: Matrix

    @MockK(relaxed = true)
    private lateinit var mockEglSurface: EGLSurface

    private val testWidth = 10
    private val testHeight = 10

    private val testTextureId = 1

    // Required even if not queried
    private val testDataY = ByteBuffer.allocateDirect(0)
    private val testDataU = ByteBuffer.allocateDirect(0)
    private val testDataV = ByteBuffer.allocateDirect(0)

    private val testStrideY = 1
    private val testStrideU = 2
    private val testStrideV = 3

    private val testViewportX = 4
    private val testViewportY = 5
    private val testViewportWidth = 6
    private val testViewportHeight = 7

    @Before
    fun setUp() {
        mockLooper = mockk()
        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockLooper
        every { mockLooper.quit() } just runs

        mockHandlerDispatcher = mockk()
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().looper } returns mockLooper
        val slot = slot<Runnable>()
        every { anyConstructed<Handler>().post(capture(slot)) } answers {
            slot.captured.run()
            true
        }

        // Previous mocks need to be done before constructor call
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockEglCoreFactory.createEglCore() } returns mockEglCore

        mockkStatic(EGL14::class)
        every { EGL14.eglCreatePbufferSurface(any(), any(), any(), any()) } returns mockEglSurface

        mockkStatic(GLES20::class)

        mockkConstructor(SurfaceTexture::class)

        mockkObject(GlUtil)
        every { GlUtil.createProgram(any(), any()) } returns 1

        mockkStatic(YuvUtil::class)
        every { YuvUtil.copyPlane(any(), any(), any(), any(), any(), any()) } just runs

        testGlVideoFrameDrawer = DefaultGlVideoFrameDrawer()
    }

    @Test
    fun `drawFrame with TEXTURE_2D calls appropriate OpenGLES function and sets viewport as expected`() {
        val testFrame = VideoFrame(0,
                VideoFrameTextureBuffer(
                        testWidth,
                        testHeight,
                        testTextureId,
                        null,
                        VideoFrameTextureBuffer.Type.TEXTURE_2D,
                        Runnable {})
        )

        testGlVideoFrameDrawer.drawFrame(
                testFrame,
                testViewportX,
                testViewportY,
                testViewportWidth,
                testViewportHeight,
                null)

        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE0) }
        verify { GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, testTextureId) }
        verify { GLES20.glViewport(testViewportX, testViewportY, testViewportWidth, testViewportHeight) }
    }

    @Test
    fun `drawFrame with TEXTURE_OES calls appropriate OpenGLES function and sets viewport as expected`() {
        val testFrame = VideoFrame(0,
                VideoFrameTextureBuffer(
                        testWidth,
                        testHeight,
                        testTextureId,
                        null,
                        VideoFrameTextureBuffer.Type.TEXTURE_OES,
                        Runnable {})
        )

        testGlVideoFrameDrawer.drawFrame(
                testFrame,
                testViewportX,
                testViewportY,
                testViewportWidth,
                testViewportHeight,
                null)

        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE0) }
        verify { GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, testTextureId) }
        verify { GLES20.glViewport(testViewportX, testViewportY, testViewportWidth, testViewportHeight) }
    }

    @Test
    fun `drawFrame with YUV buffer calls appropriate OpenGLES function and sets viewport as expected`() {
        val testFrame = VideoFrame(0,
                VideoFrameI420Buffer(
                        testWidth,
                        testHeight,
                        testDataY,
                        testDataU,
                        testDataV,
                        testStrideY,
                        testStrideU,
                        testStrideV,
                        Runnable {})
        )

        testGlVideoFrameDrawer.drawFrame(
                testFrame,
                testViewportX,
                testViewportY,
                testViewportWidth,
                testViewportHeight,
                null)

        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE0) }
        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE1) }
        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE2) }
        verify { GLES20.glViewport(testViewportX, testViewportY, testViewportWidth, testViewportHeight) }
    }

    @Test
    fun `drawFrame with RGB buffer calls appropriate OpenGLES function and sets viewport as expected`() {
        val testFrame = VideoFrame(0,
                VideoFrameRGBABuffer(
                        testWidth,
                        testHeight,
                        testDataY,
                        testStrideY,
                        Runnable {})
        )

        testGlVideoFrameDrawer.drawFrame(
                testFrame,
                testViewportX,
                testViewportY,
                testViewportWidth,
                testViewportHeight,
                null)

        verify { GLES20.glActiveTexture(GLES20.GL_TEXTURE0) }
        verify { GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, any()) }
        verify { GLES20.glViewport(testViewportX, testViewportY, testViewportWidth, testViewportHeight) }
    }

    @Test
    fun `release deletes used program to release resources`() {
        val testFrame = VideoFrame(0,
                VideoFrameRGBABuffer(
                        testWidth,
                        testHeight,
                        testDataY,
                        testStrideY,
                        Runnable {})
        )

        testGlVideoFrameDrawer.drawFrame(
                testFrame,
                testViewportX,
                testViewportY,
                testViewportWidth,
                testViewportHeight,
                null)
        testGlVideoFrameDrawer.release()

        verify { GLES20.glDeleteProgram(1) }
    }
}
