/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlin.math.abs
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultCameraCaptureSource] will configure a reasonably standard capture stream which will
 * use the [Surface] provided by the capture source provided by a [SurfaceTextureCaptureSourceFactory]
 */
class DefaultCameraCaptureSource @JvmOverloads constructor(
    private val context: Context,
    private val logger: Logger,
    private val surfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory,
    private val eventAnalyticsController: EventAnalyticsController?,
    private val cameraManager: CameraManager = context.getSystemService(
        Context.CAMERA_SERVICE
    ) as CameraManager
) : CameraCaptureSource, VideoSink {
    private val handler: Handler

    // Camera2 system library related state
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    // The following are stored from cameraCharacteristics for reuse without additional query
    // From CameraCharacteristics.SENSOR_ORIENTATION, degrees clockwise rotation
    private var sensorOrientation = 0

    // From CameraCharacteristics.LENS_FACING
    private var isCameraFrontFacing = false

    private var isCameraInterrupted = false

    // This source provides a surface we pass into the system APIs
    // and then starts emitting frames once the system starts drawing to the
    // surface. To speed up restart, since theses sources have to wait on
    // in-flight frames to finish release, we just begin the release and
    // create a new one
    private var surfaceTextureSource: SurfaceTextureCaptureSource? = null

    private val observers = ConcurrentSet.createConcurrentSet<CaptureSourceObserver>()

    // Concurrency modification could happen when sink gets
    // added/removed from another thread while sending frames
    private val sinks = ConcurrentSet.createConcurrentSet<VideoSink>()

    override val contentHint = VideoContentHint.Motion

    private val DESIRED_CAPTURE_FORMAT = VideoCaptureFormat(960, 720, 30)
    private val ROTATION_360_DEGREES = 360

    private var maxResolution: VideoResolution = VideoResolution.VideoResolutionHD

    private val TAG = "DefaultCameraCaptureSource"

    override var device: MediaDevice? = null
        set(value) {
            logger.info(TAG, "Setting capture device: $value")
            if (field == value) {
                logger.info(TAG, "Already using device: $value; ignoring")
                return
            }

            field = value

            // Restart capture if already running (i.e. we have a valid surface texture source)
            surfaceTextureSource?.let {
                stop()
                start()
            }

            device?.let {
                eventAnalyticsController?.publishEvent(EventName.videoInputSelected, mutableMapOf(
                    EventAttributeName.videoDeviceType to it.type.toString()
                ), false)
            }
        }

    init {
        try {
            // Load library so that some of webrtc definition is linked properly
            System.loadLibrary("amazon_chime_media_client")
        } catch (e: UnsatisfiedLinkError) {
            logger.error(TAG, "Unable to load native media libraries: ${e.localizedMessage}")
        }
        val thread = HandlerThread(TAG)
        thread.start()
        handler = Handler(thread.looper)

        // Initializing the device in the init block rather than at declaration to allow it to emit the corresponding meeting event.
        device = MediaDevice.listVideoDevices(cameraManager)
            .firstOrNull { it.type == MediaDeviceType.VIDEO_FRONT_CAMERA } ?: MediaDevice.listVideoDevices(cameraManager)
            .firstOrNull { it.type == MediaDeviceType.VIDEO_BACK_CAMERA }
    }

    override fun switchCamera() {
        val desiredDeviceType = if (device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA) {
            MediaDeviceType.VIDEO_BACK_CAMERA
        } else {
            MediaDeviceType.VIDEO_FRONT_CAMERA
        }
        device =
            MediaDevice.listVideoDevices(cameraManager).firstOrNull { it.type == desiredDeviceType } ?: MediaDevice.listVideoDevices(cameraManager)
                .firstOrNull { it.type == MediaDeviceType.VIDEO_BACK_CAMERA }
    }
    override fun setMaxResolution(maxResolution: VideoResolution) {
        this.maxResolution = maxResolution
    }

    override var torchEnabled: Boolean = false
        @RequiresApi(Build.VERSION_CODES.M)
        set(value) {
            if (cameraCharacteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == false) {
                logger.warn(
                    TAG,
                    "Torch not supported on current camera, setting value and returning"
                )
                return
            }

            field = value
            if (cameraDevice == null) {
                // If not in a session, use the CameraManager API
                device?.id?.let { cameraManager.setTorchMode(it, field) }
            } else {
                // Otherwise trigger a new request which will pick up the new value
                createCaptureRequest()
            }
        }

    override var format: VideoCaptureFormat = DESIRED_CAPTURE_FORMAT
        set(value) {
            logger.info(TAG, "Setting capture format: $value")
            if (field == value) {
                logger.info(TAG, "Already using format: $value; ignoring")
                return
            }

            field = VideoCaptureFormat(value.width, value.height, value.maxFps)

            // Restart capture if already running (i.e. we have a valid surface texture source)
            surfaceTextureSource?.let {
                stop()
                start()
            }
        }

    override fun start() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            handleCameraCaptureFail(CaptureSourceError.PermissionError)
            throw SecurityException("Missing necessary camera permissions")
        }
        stop()
        logger.info(TAG, "Camera capture start requested with device: $device")
        val device = device ?: run {
            logger.info(TAG, "Cannot start camera capture with null device")
            return
        }
        val id = device.id ?: run {
            logger.info(TAG, "Cannot start camera capture with null device id")
            return
        }

        cameraCharacteristics = cameraManager.getCameraCharacteristics(id).also {
            // Store these immediately for convenience
            sensorOrientation = it.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            isCameraFrontFacing =
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
        }
        val maxWidth: Int = this.maxResolution.width
        val maxHeight: Int = this.maxResolution.height
        val maxFps: Int = 30

        val chosenCaptureFormat: VideoCaptureFormat? =
            MediaDevice.listSupportedVideoCaptureFormats(cameraManager, device, maxFps, maxWidth, maxHeight).minByOrNull { format ->
                abs(format.width - this.format.width) + abs(format.height - this.format.height)
            }
        val surfaceTextureFormat: VideoCaptureFormat = chosenCaptureFormat ?: run {
            handleCameraCaptureFail(CaptureSourceError.ConfigurationFailure)
            return
        }
        surfaceTextureSource =
            surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
                surfaceTextureFormat.width,
                surfaceTextureFormat.height,
                contentHint
            )
        surfaceTextureSource?.addVideoSink(this)
        surfaceTextureSource?.start()

        cameraManager.openCamera(id, cameraDeviceStateCallback, handler)
    }

    override fun stop() {
        logger.info(TAG, "Stopping camera capture source")
        val sink: VideoSink = this
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            // Close camera capture session
            cameraCaptureSession?.close()
            cameraCaptureSession = null

            // Close camera device, this will eventually trigger the stop callback
            cameraDevice?.close()
            cameraDevice = null

            // Stop surface capture source
            surfaceTextureSource?.removeVideoSink(sink)
            surfaceTextureSource?.stop()
            surfaceTextureSource?.release()
            surfaceTextureSource = null
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        val processedBuffer: VideoFrameBuffer = createBufferWithUpdatedTransformMatrix(
            frame.buffer as VideoFrameTextureBuffer,
            isCameraFrontFacing, -sensorOrientation
        )

        val processedFrame =
            VideoFrame(frame.timestampNs, processedBuffer, getCaptureFrameRotation())
        sinks.iterator().forEach { it.onVideoFrameReceived(processedFrame) }
        processedBuffer.release()
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.add(observer)
    }

    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.remove(observer)
    }

    fun release() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Stopping handler looper")
            handler.removeCallbacksAndMessages(null)
            handler.looper.quit()
        }
    }

    // Implement and store callbacks as private constants since we can't inherit from all of them
    // due to Kotlin not allowing multiple class inheritance

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            logger.info(TAG, "Camera device opened for ID ${device.id}")
            cameraDevice = device

            if (isCameraInterrupted) {
                isCameraInterrupted = false
                eventAnalyticsController?.publishEvent(EventName.videoCaptureSessionInterruptionEnded, mutableMapOf(), false)
            }

            try {
                cameraDevice?.createCaptureSession(
                    listOf(surfaceTextureSource?.surface),
                    cameraCaptureSessionStateCallback,
                    handler
                )
            } catch (exception: CameraAccessException) {
                logger.info(
                    TAG,
                    "Exception encountered creating capture session: ${exception.reason}"
                )
                handleCameraCaptureFail(CaptureSourceError.SystemFailure)
                return
            }
        }

        override fun onClosed(device: CameraDevice) {
            logger.info(TAG, "Camera device closed for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
        }

        override fun onDisconnected(device: CameraDevice) {
            logger.info(TAG, "Camera device disconnected for ID ${device.id}")
            isCameraInterrupted = true
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
            eventAnalyticsController?.publishEvent(EventName.videoCaptureSessionInterruptionBegan, mutableMapOf(), false)
        }

        override fun onError(device: CameraDevice, error: Int) {
            logger.info(TAG, "Camera device encountered error: $error for ID ${device.id}")
            handleCameraCaptureFail(CaptureSourceError.SystemFailure)
        }
    }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            logger.info(
                TAG,
                "Camera capture session configured for session with device ID: ${session.device.id}"
            )
            cameraCaptureSession = session
            createCaptureRequest()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.error(
                TAG, "Camera session configuration failed with device ID: ${session.device.id}"
            )
            handleCameraCaptureFail(CaptureSourceError.ConfigurationFailure)
            session.close()
        }
    }

    private fun handleCameraCaptureFail(error: CaptureSourceError) {
        val attributes = mutableMapOf<EventAttributeName, Any>(
            EventAttributeName.videoInputErrorMessage to error
        )
        eventAnalyticsController?.publishEvent(EventName.videoInputFailed, attributes)
        ObserverUtils.notifyObserverOnMainThread(observers) {
            it.onCaptureFailed(error)
        }
    }

    private val cameraCaptureSessionCaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                logger.error(TAG, "Camera capture session failed: $failure")
                handleCameraCaptureFail(CaptureSourceError.SystemFailure)
            }
        }

    private fun createCaptureRequest() {
        val cameraDevice = cameraDevice ?: run {
            // This can occur occasionally if capture is restarted before the previous
            // completes. The next request will complete normally.
            logger.warn(TAG, "createCaptureRequest called without device set, may be mid restart")
            return
        }
        try {
            val captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            // Set target FPS
            val fpsRanges: Array<Range<Int>> =
                cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?: run {
                        logger.error(TAG, "Could not retrieve camera FPS ranges")
                        handleCameraCaptureFail(CaptureSourceError.ConfigurationFailure)
                        return
                    }
            // Pick range with max closest to but not exceeding the set max framerate
            val bestFpsRange = fpsRanges
                    .filter { it.upper <= this.format.maxFps }
                    .minByOrNull { this.format.maxFps - it.upper }
                    ?: run {
                        logger.warn(TAG, "No FPS ranges below set max FPS")
                        // Just fall back to the closest
                        return@run fpsRanges.minByOrNull { abs(this.format.maxFps - it.upper) }
                    } ?: run {
                        logger.error(TAG, "No valid FPS ranges")
                        handleCameraCaptureFail(CaptureSourceError.ConfigurationFailure)
                        return
                    }

            logger.info(TAG, "Setting target FPS range to $bestFpsRange")
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(bestFpsRange.lower, bestFpsRange.upper)
            )

            // Set target auto exposure mode
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
            )
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false)

            // Set current torch status
            if (torchEnabled) {
                captureRequestBuilder.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH
                )
            } else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }

            setStabilizationMode(captureRequestBuilder)
            setFocusMode(captureRequestBuilder)

            captureRequestBuilder.addTarget(
                surfaceTextureSource?.surface
                    ?: throw UnknownError("Surface texture source should not be null")
            )
            cameraCaptureSession?.setRepeatingRequest(
                captureRequestBuilder.build(), cameraCaptureSessionCaptureCallback, handler
            )
            logger.info(
                TAG,
                "Capture request completed with device ID: ${cameraCaptureSession?.device?.id}"
            )
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureStarted()
            }
        } catch (exception: CameraAccessException) {
            logger.error(
                TAG,
                "Failed to start capture request with device ID: ${cameraCaptureSession?.device?.id}, exception:$exception"
            )
            handleCameraCaptureFail(CaptureSourceError.SystemFailure)
            return
        }
    }

    private fun setStabilizationMode(captureRequestBuilder: CaptureRequest.Builder) {
        if (cameraCharacteristics?.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
            )?.any { it == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON } == true
        ) {
            captureRequestBuilder[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE] =
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
            captureRequestBuilder[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] =
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            logger.info(TAG, "Using optical stabilization.")
            return
        }

        // If no optical mode is available, try software.
        if (cameraCharacteristics?.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            )?.any { it == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON } == true
        ) {
            captureRequestBuilder[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] =
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            captureRequestBuilder[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE] =
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
            logger.info(TAG, "Using video stabilization.")
            return
        }

        logger.info(TAG, "Stabilization not available.")
    }

    private fun setFocusMode(captureRequestBuilder: CaptureRequest.Builder) {
        if (cameraCharacteristics?.get(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )?.any { it == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO } == true
        ) {
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            logger.info(TAG, "Using optical stabilization.")
            return
        }

        logger.info(TAG, "Auto-focus is not available.")
    }

    private fun getCaptureFrameRotation(): VideoRotation {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var rotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_0 -> 0
            else -> 0
        }
        // Account for front cammera mirror
        if (!isCameraFrontFacing) {
            rotation = ROTATION_360_DEGREES - rotation
        }
        // Account for physical camera orientation
        rotation = (sensorOrientation + rotation) % ROTATION_360_DEGREES
        return VideoRotation.from(rotation) ?: VideoRotation.Rotation0
    }

    private fun createBufferWithUpdatedTransformMatrix(
        buffer: VideoFrameTextureBuffer,
        mirror: Boolean,
        rotation: Int
    ): VideoFrameTextureBuffer {
        val transformMatrix = Matrix()
        // Perform mirror and rotation around (0.5, 0.5) since that is the center of the texture.
        transformMatrix.preTranslate(0.5f, 0.5f)
        if (mirror) {
            // This negative scale mirrors across the vertical axis
            transformMatrix.preScale(-1f, 1f)
        }
        transformMatrix.preRotate(rotation.toFloat())
        transformMatrix.preTranslate(-0.5f, -0.5f)

        // The width and height are not affected by rotation
        val newMatrix = Matrix(buffer.transformMatrix)
        newMatrix.preConcat(transformMatrix)
        buffer.retain()
        return VideoFrameTextureBuffer(
            buffer.width,
            buffer.height,
            buffer.textureId,
            newMatrix,
            buffer.type,
            Runnable { buffer.release() })
    }
}
