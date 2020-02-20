/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.devicecontroller

import android.media.AudioDeviceInfo

/**
 * Media device with its info.
 *
 * @property label human readable string describing the device.
 * @property type media device type e.g. 2 (Build-in speaker)
 */
data class MediaDevice(val label: String, val type: Int) {
    val order: Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 0
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 1
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 2
        AudioDeviceInfo.TYPE_TELEPHONY -> 3
        else -> 99
    }

    override fun toString(): String = label
}
