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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultCameraCaptureSource] will configure a reasonably standard capture stream which will
 * use the [Surface] provided by the capture source provided by a [SurfaceTextureCaptureSourceFactory]
 */
class DefaultCameraCaptureSource(
    private val context: Context,
    private val logger: Logger,
    private val surfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory,
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

    // This source provides a surface we pass into the system APIs
    // and then starts emitting frames once the system starts drawing to the
    // surface. To speed up restart, since theses sources have to wait on
    // in-flight frames to finish release, we just begin the release and
    // create a new one
    private var surfaceTextureSource: SurfaceTextureCaptureSource? = null

    private val observers = mutableSetOf<CaptureSourceObserver>()
    private val sinks = mutableSetOf<VideoSink>()

    override val contentHint = VideoContentHint.Motion

    private val MAX_INTERNAL_SUPPORTED_FPS = 15
    private val DESIRED_CAPTURE_FORMAT = VideoCaptureFormat(960, 720, MAX_INTERNAL_SUPPORTED_FPS)
    private val ROTATION_360_DEGREES = 360

    private val TAG = "DefaultCameraCaptureSource"

    init {
        val thread = HandlerThread("DefaultCameraCaptureSource")
        thread.start()
        handler = Handler(thread.looper)
    }

    override var device: MediaDevice? = MediaDevice.listVideoDevices(cameraManager)
        .firstOrNull { it.type == MediaDeviceType.VIDEO_FRONT_CAMERA } ?: MediaDevice.listVideoDevices(cameraManager)
        .firstOrNull { it.type == MediaDeviceType.VIDEO_BACK_CAMERA }
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

            if (value.maxFps > MAX_INTERNAL_SUPPORTED_FPS) {
                logger.info(TAG, "Limiting capture to 15 FPS to avoid frame drops")
            }
            field = VideoCaptureFormat(value.width, value.height, min(value.maxFps,
                MAX_INTERNAL_SUPPORTED_FPS
            ))

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
            throw SecurityException("Missing necessary camera permissions")
        }

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

        val chosenCaptureFormat: VideoCaptureFormat? =
                MediaDevice.listSupportedVideoCaptureFormats(cameraManager, device).minBy { format ->
                    abs(format.width - this.format.width) + abs(format.height - this.format.height)
                }
        val surfaceTextureFormat: VideoCaptureFormat = chosenCaptureFormat ?: run {
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
            }
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
        sinks.forEach { it.onVideoFrameReceived(processedFrame) }
        processedBuffer.release()
    }

    override fun addVideoSink(sink: VideoSink) {
        handler.post { sinks.add(sink) }
    }

    override fun removeVideoSink(sink: VideoSink) {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            sinks.remove(sink)
        }
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
            try {
                cameraDevice?.createCaptureSession(
                        listOf(surfaceTextureSource?.surface),
                        cameraCaptureSessionStateCallback,
                        handler
                )
            } catch (exception: CameraAccessException) {
                logger.info(TAG, "Exception encountered creating capture session: ${exception.reason}")
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onCaptureFailed(
                            CaptureSourceError.SystemFailure
                    )
                }
                return
            }
        }

        override fun onClosed(device: CameraDevice) {
            logger.info(TAG, "Camera device closed for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
        }

        override fun onDisconnected(device: CameraDevice) {
            logger.info(TAG, "Camera device disconnected for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
        }

        override fun onError(device: CameraDevice, error: Int) {
            logger.info(TAG, "Camera device encountered error: $error for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.SystemFailure)
            }
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
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
            }
            session.close()
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
                    ObserverUtils.notifyObserverOnMainThread(observers) {
                        it.onCaptureFailed(CaptureSourceError.SystemFailure)
                    }
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
            val fpsRanges: Array<Range<Int>> = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?: run {
                        logger.error(TAG, "Could not retrieve camera FPS ranges")
                        ObserverUtils.notifyObserverOnMainThread(observers) {
                            it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
                        }
                        return
                    }
            // Pick range with max closest to but not exceeding the set max framerate
            val bestFpsRange = fpsRanges
                    .filter { it.upper <= this.format.maxFps }
                    .minBy { this.format.maxFps - it.upper }
                    ?: run {
                        logger.warn(TAG, "No FPS ranges below set max FPS")
                        // Just fall back to the closest
                        return@run fpsRanges.minBy { abs(this.format.maxFps - it.upper) }
                    } ?: run {
                        logger.error(TAG, "No valid FPS ranges")
                        ObserverUtils.notifyObserverOnMainThread(observers) {
                            it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
                        }
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

            captureRequestBuilder.addTarget(surfaceTextureSource?.surface ?: throw UnknownError("Surface texture source should not be null"))
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
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.SystemFailure)
            }
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
