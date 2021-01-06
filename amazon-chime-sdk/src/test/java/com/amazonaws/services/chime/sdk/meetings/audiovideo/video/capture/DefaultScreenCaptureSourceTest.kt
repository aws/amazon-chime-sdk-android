/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Display
import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultScreenCaptureSourceTest {

    @MockK(relaxed = true)
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockSurfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory

    @MockK(relaxed = true)
    private lateinit var mockDisplayManager: DisplayManager

    @MockK
    private lateinit var mockMediaProjectionManager: MediaProjectionManager

    private var activityResult = 1

    @MockK
    private lateinit var mockActivityData: Intent

    @InjectMockKs
    private lateinit var testScreenCaptureSource: DefaultScreenCaptureSource

    private lateinit var mockLooper: Looper

    @MockK(relaxed = true)
    private lateinit var mockSurfaceTextureCaptureSource: SurfaceTextureCaptureSource

    @MockK
    private lateinit var mockVideoSink: VideoSink

    @MockK
    private lateinit var mockMediaProjection: MediaProjection

    @MockK
    private lateinit var mockVirtualDisplay: VirtualDisplay

    @MockK
    private lateinit var mockCaptureSourceObserver: CaptureSourceObserver

    @MockK
    private lateinit var mockFrame: VideoFrame

    @MockK
    private lateinit var mockDisplay: Display

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        mockLooper = mockk()
        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockLooper
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().looper } returns mockLooper
        val slot = slot<Runnable>()
        every { anyConstructed<Handler>().post(capture(slot)) } answers {
            slot.captured.run()
            true
        }
        mockkStatic(Looper::class)
        every { Looper.myLooper() } returns mockLooper

        Dispatchers.setMain(testDispatcher)

        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockSurfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(any(), any(), any()) } returns mockSurfaceTextureCaptureSource
        every { mockContext.getSystemService(Context.DISPLAY_SERVICE) } returns mockDisplayManager
        every { mockContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) } returns mockMediaProjectionManager
        every { mockMediaProjectionManager.getMediaProjection(1, mockActivityData) } returns mockMediaProjection
        every { mockMediaProjection.createVirtualDisplay(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockVirtualDisplay
        every { mockDisplayManager.getDisplay(Display.DEFAULT_DISPLAY) } returns mockDisplay
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `start will notify subscribed observers of onCaptureStarted event`() {
        every { mockDisplay.rotation } returns Surface.ROTATION_0
        testScreenCaptureSource.addCaptureSourceObserver(mockCaptureSourceObserver)

        testScreenCaptureSource.start()

        verify(exactly = 1) { mockCaptureSourceObserver.onCaptureStarted() }
    }

    @Test
    fun `stop will notify subscribed observers of onCaptureStopped event`() {
        testScreenCaptureSource.addCaptureSourceObserver(mockCaptureSourceObserver)

        testScreenCaptureSource.stop()

        verify(exactly = 1) { mockCaptureSourceObserver.onCaptureStopped() }
    }

    @Test
    fun `restarting for rotation will not notify subscribed observers of events`() {
        every { mockDisplay.rotation } returns Surface.ROTATION_90

        testScreenCaptureSource.onVideoFrameReceived(mockFrame)

        verify(exactly = 1) { mockMediaProjection.stop() }
        verify(exactly = 1) { mockMediaProjectionManager.getMediaProjection(1, mockActivityData) }
        verify(exactly = 0) { mockCaptureSourceObserver.onCaptureStopped() }
        verify(exactly = 0) { mockCaptureSourceObserver.onCaptureStarted() }
    }
}
