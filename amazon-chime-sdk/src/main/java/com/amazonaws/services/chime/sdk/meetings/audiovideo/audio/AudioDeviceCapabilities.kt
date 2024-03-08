/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

/**
 * [AudioDeviceCapabilities] describes whether the audio input and output devices are enabled or disabled.
 */
enum class AudioDeviceCapabilities(val value: Int) {
    /**
     * Disable both the audio input and output devices (i.e. connections to the microphone and speaker devices are not
     * opened). Muted packets are sent to the server.
     */
    None(0),

    /**
     * Disable the audio input device and only enable the audio output device (i.e. the connection to the microphone
     * device is not opened). Muted packets are sent to the server.
     */
    OutputOnly(1),

    /**
     * Enable both the audio input and output devices.
     */
    InputAndOutput(2);

    companion object {
        fun from(intValue: Int): AudioDeviceCapabilities? = values().find { it.value == intValue }
        fun from(intValue: Int, defaultAudioDeviceCapabilities: AudioDeviceCapabilities): AudioDeviceCapabilities = from(intValue) ?: defaultAudioDeviceCapabilities
    }
}
