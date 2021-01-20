/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.content.Context
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Range
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore

class DefaultCameraCaptureSourceTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockSurfaceTextureCaptureSourceFactory: DefaultSurfaceTextureCaptureSourceFactory

    @MockK
    private lateinit var mockCameraManager: CameraManager

    @InjectMockKs
    private lateinit var testCameraCaptureSource: DefaultCameraCaptureSource

    private lateinit var mockLooper: Looper

    @MockK
    private lateinit var mockFrontCameraCharacteristics: CameraCharacteristics

    @MockK
    private lateinit var mockBackCameraCharacteristics: CameraCharacteristics

    @MockK(relaxed = true)
    private lateinit var mockSurfaceTextureCaptureSource: SurfaceTextureCaptureSource

    @MockK(relaxed = true)
    private lateinit var mockMatrix: Matrix

    @MockK
    private lateinit var mockVideoSink: VideoSink

    @MockK(relaxed = true)
    private lateinit var mockWindowManager: WindowManager

    @MockK(relaxed = true)
    private lateinit var mockCameraDevice: CameraDevice

    @MockK(relaxed = true)
    private lateinit var mockCameraSession: CameraCaptureSession

    @MockK
    private lateinit var mockObserver: CaptureSourceObserver

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        // Setup handler/thread/looper mocking
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

        mockkObject(MediaDevice.Companion)
        every { MediaDevice.listVideoDevices(any()) } returns listOf(
            MediaDevice("front", MediaDeviceType.VIDEO_FRONT_CAMERA, "0"),
            MediaDevice("back", MediaDeviceType.VIDEO_BACK_CAMERA, "1")
        )
        every { MediaDevice.listSupportedVideoCaptureFormats(any(), any()) } returns listOf(
            VideoCaptureFormat(1280, 720, 15)
        )

        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns 0

        mockkConstructor(Matrix::class)
        every { anyConstructed<Matrix>().preTranslate(any(), any()) } returns true
        every { anyConstructed<Matrix>().preScale(any(), any()) } returns true
        every { anyConstructed<Matrix>().preRotate(any()) } returns true
        every { anyConstructed<Matrix>().preConcat(any()) } returns true

        Dispatchers.setMain(testDispatcher)

        // Most of the previous mocks need to be done before constructor call
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockCameraManager.getCameraCharacteristics("0") } returns mockFrontCameraCharacteristics
        every { mockCameraManager.getCameraCharacteristics("1") } returns mockBackCameraCharacteristics
        every { mockCameraManager.openCamera(any(), any<CameraDevice.StateCallback>(), any()) } just runs
        every { mockSurfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(any(), any(), any()) } returns mockSurfaceTextureCaptureSource
        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns arrayOf(Range(15, 15))
        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 270
        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraMetadata.LENS_FACING_FRONT
        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) } returns null
        every { mockBackCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns arrayOf(Range(15, 15))
        every { mockBackCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 180
        every { mockBackCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraMetadata.LENS_FACING_BACK
        every { mockContext.getSystemService(Context.WINDOW_SERVICE) } returns mockWindowManager
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `start creates and starts surface source, and calls CameraManager openCamera`() {
        testCameraCaptureSource.start()

        verify { mockSurfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(1280, 720, VideoContentHint.Motion) }
        verify { mockSurfaceTextureCaptureSource.start() }
        verify { mockSurfaceTextureCaptureSource.addVideoSink(any()) }
        verify { mockCameraManager.openCamera("0", any<CameraDevice.StateCallback>(), any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `switch should default to back if front camera is missing`() {
        testCameraCaptureSource.start()
        testCameraCaptureSource.switchCamera()
        every { MediaDevice.listVideoDevices(any()) } returns listOf(
            MediaDevice("back", MediaDeviceType.VIDEO_BACK_CAMERA, "1")
        ) andThen listOf(
            MediaDevice("back", MediaDeviceType.VIDEO_BACK_CAMERA, "1")
        )

        Assert.assertEquals(MediaDeviceType.VIDEO_BACK_CAMERA, testCameraCaptureSource.device?.type)

        testCameraCaptureSource.switchCamera()

        Assert.assertEquals(MediaDeviceType.VIDEO_BACK_CAMERA, testCameraCaptureSource.device?.type)
}

    @Ignore("Broken on build server, possible Mockk issue")
    fun `stop stops and releases surface texture capture source`() {
        testCameraCaptureSource.start()
        testCameraCaptureSource.stop()

        verify { mockSurfaceTextureCaptureSource.removeVideoSink(any()) }
        verify { mockSurfaceTextureCaptureSource.stop() }
        verify { mockSurfaceTextureCaptureSource.release() }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `setting device will restart source`() {
        testCameraCaptureSource.start()
        testCameraCaptureSource.device = MediaDevice("back", MediaDeviceType.VIDEO_BACK_CAMERA, "1")

        verify(exactly = 1) { mockCameraManager.openCamera("0", any<CameraDevice.StateCallback>(), any()) }
        verify(exactly = 1) { mockCameraManager.openCamera("1", any<CameraDevice.StateCallback>(), any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `switchCamera will switch to back camera`() {
        testCameraCaptureSource.start()
        testCameraCaptureSource.switchCamera()

        verify(exactly = 1) { mockCameraManager.openCamera("0", any<CameraDevice.StateCallback>(), any()) }
        verify(exactly = 1) { mockCameraManager.openCamera("1", any<CameraDevice.StateCallback>(), any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `capturer will pass through frames`() {
        testCameraCaptureSource.addVideoSink(mockVideoSink)
        testCameraCaptureSource.start()
        val testFrame = VideoFrame(0,
            VideoFrameTextureBuffer(
                1280,
                720,
                1,
                mockMatrix,
                VideoFrameTextureBuffer.Type.TEXTURE_2D,
                Runnable {})
        )
        testCameraCaptureSource.onVideoFrameReceived(testFrame)

        verify(exactly = 1) { mockVideoSink.onVideoFrameReceived(any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `onOpened triggers camera device createCaptureSession`() {
        val slot = slot<CameraDevice.StateCallback>()
        every { mockCameraManager.openCamera(any(), capture(slot), any()) } just runs

        testCameraCaptureSource.start()

        slot.captured.onOpened(mockCameraDevice)

        verify { mockCameraDevice.createCaptureSession(any(), any(), any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `onConfigured triggers setRepeatingRequest`() {
        val stateCallbackSlot = slot<CameraDevice.StateCallback>()
        every { mockCameraManager.openCamera(any(), capture(stateCallbackSlot), any()) } just runs
        val sessionCallbackSlot = slot<CameraCaptureSession.StateCallback>()
        every { mockCameraDevice.createCaptureSession(any(), capture(sessionCallbackSlot), any()) } just runs

        testCameraCaptureSource.start()

        stateCallbackSlot.captured.onOpened(mockCameraDevice)
        sessionCallbackSlot.captured.onConfigured(mockCameraSession)

        verify { mockCameraSession.setRepeatingRequest(any(), any(), any()) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `No FPS ranges trigger configuration error`() {
        val stateCallbackSlot = slot<CameraDevice.StateCallback>()
        every { mockCameraManager.openCamera(any(), capture(stateCallbackSlot), any()) } just runs
        val sessionCallbackSlot = slot<CameraCaptureSession.StateCallback>()
        every { mockCameraDevice.createCaptureSession(any(), capture(sessionCallbackSlot), any()) } just runs

        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns null

        testCameraCaptureSource.addCaptureSourceObserver(mockObserver)
        testCameraCaptureSource.start()

        stateCallbackSlot.captured.onOpened(mockCameraDevice)
        sessionCallbackSlot.captured.onConfigured(mockCameraSession)

        verify { mockObserver.onCaptureFailed(CaptureSourceError.ConfigurationFailure) }
    }

    @Ignore("Broken on build server, possible Mockk issue")
    fun `FPS range above max value still is selected`() {
        val stateCallbackSlot = slot<CameraDevice.StateCallback>()
        every { mockCameraManager.openCamera(any(), capture(stateCallbackSlot), any()) } just runs
        val sessionCallbackSlot = slot<CameraCaptureSession.StateCallback>()
        every { mockCameraDevice.createCaptureSession(any(), capture(sessionCallbackSlot), any()) } just runs

        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) } returns arrayOf(Range(30, 30))

        testCameraCaptureSource.addCaptureSourceObserver(mockObserver)
        testCameraCaptureSource.start()

        stateCallbackSlot.captured.onOpened(mockCameraDevice)
        sessionCallbackSlot.captured.onConfigured(mockCameraSession)

        verify { mockObserver.onCaptureFailed(CaptureSourceError.ConfigurationFailure) }
    }
}
