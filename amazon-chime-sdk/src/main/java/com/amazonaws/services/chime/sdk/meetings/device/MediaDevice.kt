/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.VideoCaptureFormat

/**
 * Media device with its info.
 *
 * @property label: String - human readable string describing the device.
 * @property type: [MediaDeviceType] - media device type
 * @property id: String - Unique ID, if applicable
 */
data class MediaDevice(
    val label: String,
    val type: MediaDeviceType,
    val id: String? = null
) {
    val order: Int = when (type) {
        MediaDeviceType.AUDIO_BLUETOOTH -> 0
        MediaDeviceType.AUDIO_WIRED_HEADSET -> 1
        MediaDeviceType.AUDIO_USB_HEADSET -> 1
        MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> 2
        MediaDeviceType.AUDIO_HANDSET -> 3
        MediaDeviceType.VIDEO_FRONT_CAMERA -> 4
        MediaDeviceType.VIDEO_BACK_CAMERA -> 5
        MediaDeviceType.VIDEO_EXTERNAL_CAMERA -> 6
        else -> 99
    }

    override fun toString(): String = label

    companion object {

        private val DEFAULT_MAX_VIDEO_FORMAT_FPS = 30
        private val DEFAULT_MAX_VIDEO_WIDTH = 1280
        private val DEFAULT_MAX_VIDEO_HEIGHT = 720

        /**
         * Lists currently available video devices.
         *
         * @param cameraManager: [CameraManager] - Camera manager to use for enumeration
         *
         * @return [List<MediaDevice>] - A list of currently available video devices.
         */
        fun listVideoDevices(cameraManager: CameraManager): List<MediaDevice> {
            return cameraManager.cameraIdList.map { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING)?.let {
                    val type = MediaDeviceType.fromCameraMetadata(it)
                    return@map MediaDevice("$id ($type)", type, id)
                }
                return@map MediaDevice("$id ($MediaDeviceType.OTHER)", MediaDeviceType.OTHER, id)
            }
        }

        /**
         * Lists currently available video devices.
         *
         * @param cameraManager: [CameraManager] - Camera manager to use for enumeration
         * @param mediaDevice: [MediaDevice] - Media device to inspect
         * @param maxVideoFps: Int - Desired max fps
         * @return [List<VideoCaptureFormat>] - A list of supported formats for the given device
         */
        fun listSupportedVideoCaptureFormats(
            cameraManager: CameraManager,
            mediaDevice: MediaDevice,
            maxVideoFps: Int = DEFAULT_MAX_VIDEO_FORMAT_FPS
        ): List<VideoCaptureFormat> {
            val characteristics = cameraManager.getCameraCharacteristics(mediaDevice.id ?: return emptyList())
            val fps = characteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )?.map { it.upper }?.filter { it <= maxVideoFps }?.max() ?: DEFAULT_MAX_VIDEO_FORMAT_FPS

            val streamMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            ?: return emptyList()
            val nativeSizes = streamMap.getOutputSizes(SurfaceTexture::class.java)
                    ?: return emptyList()
            val filterList = nativeSizes.filter { it.width <= DEFAULT_MAX_VIDEO_WIDTH && it.height <= DEFAULT_MAX_VIDEO_HEIGHT }
            return filterList.map { size -> VideoCaptureFormat(size.width, size.height, fps) }
        }
    }
}

/**
 * The media device's type (Ex: video front camera, video rear camera, audio bluetooth)
 */
enum class MediaDeviceType {
    AUDIO_BLUETOOTH,
    AUDIO_WIRED_HEADSET,
    AUDIO_USB_HEADSET,
    AUDIO_BUILTIN_SPEAKER,
    AUDIO_HANDSET,
    VIDEO_FRONT_CAMERA,
    VIDEO_BACK_CAMERA,
    VIDEO_EXTERNAL_CAMERA,
    OTHER;

    override fun toString(): String {
        return when (this) {
            AUDIO_BLUETOOTH -> "Bluetooth"
            AUDIO_WIRED_HEADSET -> "Wired Headset"
            AUDIO_USB_HEADSET -> "USB Headset"
            AUDIO_BUILTIN_SPEAKER -> "Builtin Speaker"
            AUDIO_HANDSET -> "Handset"
            VIDEO_FRONT_CAMERA -> "Front Camera"
            VIDEO_BACK_CAMERA -> "Back Camera"
            VIDEO_EXTERNAL_CAMERA -> "External Camera"
            OTHER -> "Other"
        }
    }

    companion object {
        fun fromAudioDeviceInfo(audioDeviceInfo: Int): MediaDeviceType {
            return when (audioDeviceInfo) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AUDIO_BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AUDIO_WIRED_HEADSET
                AudioDeviceInfo.TYPE_USB_HEADSET -> AUDIO_USB_HEADSET
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AUDIO_BUILTIN_SPEAKER
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                AudioDeviceInfo.TYPE_TELEPHONY -> AUDIO_HANDSET
                else -> OTHER
            }
        }

        fun fromCameraMetadata(cameraMetadata: Int): MediaDeviceType {
            return when (cameraMetadata) {
                CameraMetadata.LENS_FACING_FRONT -> VIDEO_FRONT_CAMERA
                CameraMetadata.LENS_FACING_BACK -> VIDEO_BACK_CAMERA
                CameraMetadata.LENS_FACING_EXTERNAL -> VIDEO_EXTERNAL_CAMERA
                else -> OTHER
            }
        }
    }
}
