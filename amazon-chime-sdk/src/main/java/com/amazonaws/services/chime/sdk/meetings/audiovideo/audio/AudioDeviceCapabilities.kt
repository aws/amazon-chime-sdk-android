/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

import android.Manifest

/**
 * [AudioDeviceCapabilities] describes whether the audio input and output devices are enabled or disabled. Disabling
 * either the audio input or output will change what audio permissions are required in order to join a meeting.
 */
enum class AudioDeviceCapabilities {
    /**
     * Disable both the audio input and output devices (i.e. connections to the microphone and speaker devices are not
     * opened). Muted packets are sent to the server. No audio permissions are required.
     */
    None,

    /**
     * Disable the audio input device and only enable the audio output device (i.e. the connection to the microphone
     * device is not opened). Muted packets are sent to the server. MODIFY_AUDIO_SETTINGS permission is required.
     */
    OutputOnly,

    /**
     * Enable both the audio input and output devices. MODIFY_AUDIO_SETTINGS and RECORD_AUDIO permissions are required.
     */
    InputAndOutput;

    private val audioInputPermissions = arrayOf(Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.RECORD_AUDIO)
    private val audioOutputPermissions = arrayOf(Manifest.permission.MODIFY_AUDIO_SETTINGS)

    fun requiredPermissions(): Array<String> = when (this) {
        None -> emptyArray()
        OutputOnly -> audioOutputPermissions
        InputAndOutput -> (audioInputPermissions + audioOutputPermissions).distinct().toTypedArray()
    }
}
