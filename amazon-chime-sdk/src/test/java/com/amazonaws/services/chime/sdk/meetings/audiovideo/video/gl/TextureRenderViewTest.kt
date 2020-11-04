/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.content.Context
import android.graphics.Matrix
import android.opengl.EGL14
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.android.HandlerDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

class TextureRenderViewTest {
    @MockK
    private lateinit var mockContext: Context

    private lateinit var testEglVideoRenderView: TextureRenderView

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    private lateinit var mockHandlerDispatcher: HandlerDispatcher

    @MockK(relaxed = true)
    private lateinit var mockEglCore: EglCore

    private lateinit var mockLooper: Looper

    @MockK
    private lateinit var mockMatrix: Matrix

    @MockK
    private lateinit var mockHolder: SurfaceHolder

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Setup handler/thread/looper mocking
        mockLooper = mockk()
        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockLooper
        mockHandlerDispatcher = mockk()
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().looper } returns mockLooper
        val slot = slot<Runnable>()
        every { anyConstructed<Handler>().post(capture(slot)) } answers {
            slot.captured.run()
            true
        }
        mockkStatic(Looper::class)
        every { Looper.myLooper() } returns mockLooper
        every { mockLooper.quitSafely() } just runs
        Dispatchers.setMain(Dispatchers.Unconfined)

        mockkConstructor(SurfaceView::class)
        // For some reason the following has no effect
        every { anyConstructed<SurfaceView>().holder } returns mockHolder
        every { mockHolder.addCallback(any()) } just runs

        every { mockEglCoreFactory.createEglCore() } returns mockEglCore

        // Create actual test object
        testEglVideoRenderView = TextureRenderView(mockContext)
    }

    @Test
    fun `init creates and starts thread, creates eglCore`() {
        testEglVideoRenderView.init(mockEglCoreFactory)

        verify { anyConstructed<HandlerThread>().start() }
        verify { mockEglCoreFactory.createEglCore() }
    }

    @Test
    fun `release releases eglCore and stops thread`() {
        testEglVideoRenderView.init(mockEglCoreFactory)
        testEglVideoRenderView.release()

        verify { mockEglCore.release() }
        verify { mockLooper.quitSafely() }
    }

    @Test
    fun `onReceiveVideoFrame eventually calls eglSwapBuffers`() {
        mockkStatic(EGL14::class)

        testEglVideoRenderView.init(mockEglCoreFactory)
        val testFrame = VideoFrame(0,
                VideoFrameTextureBuffer(
                        1280,
                        720,
                        1,
                        mockMatrix,
                        VideoFrameTextureBuffer.Type.TEXTURE_OES,
                        Runnable {})
        )

        testEglVideoRenderView.onVideoFrameReceived(testFrame)

        verify { EGL14.eglSwapBuffers(any(), any()) }
    }
}
