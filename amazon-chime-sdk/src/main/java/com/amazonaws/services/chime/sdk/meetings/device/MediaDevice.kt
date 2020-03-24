/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.media.AudioDeviceInfo

/**
 * Media device with its info.
 *
 * @property label: String - human readable string describing the device.
 * @property type: [MediaDeviceType] - media device type
 */
data class MediaDevice(val label: String, val type: MediaDeviceType) {
    val order: Int = when (type) {
        MediaDeviceType.AUDIO_BLUETOOTH -> 0
        MediaDeviceType.AUDIO_WIRED_HEADSET -> 1
        MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> 2
        MediaDeviceType.AUDIO_HANDSET -> 3
        else -> 99
    }

    override fun toString(): String = label
}

/**
 * The media device's type (Ex: video front camera, video rear camera, audio bluetooth)
 */
enum class MediaDeviceType {
    AUDIO_BLUETOOTH,
    AUDIO_WIRED_HEADSET,
    AUDIO_BUILTIN_SPEAKER,
    AUDIO_HANDSET,
    VIDEO_FRONT_CAMERA,
    VIDEO_BACK_CAMERA,
    OTHER;

    companion object {
        fun fromAudioDeviceInfo(audioDeviceInfo: Int): MediaDeviceType {
            return when (audioDeviceInfo) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AUDIO_BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AUDIO_WIRED_HEADSET
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AUDIO_BUILTIN_SPEAKER
                AudioDeviceInfo.TYPE_TELEPHONY -> AUDIO_HANDSET
                else -> OTHER
            }
        }
    }
}
