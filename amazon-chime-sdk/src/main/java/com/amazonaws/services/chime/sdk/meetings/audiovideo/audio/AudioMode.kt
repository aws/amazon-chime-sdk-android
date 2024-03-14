/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

/**
 * [AudioMode] describes the audio mode in which the audio client should operate during a meeting session.
 */
enum class AudioMode(val value: Int) {
    /**
     * The mono audio mode with single audio channel and 16KHz sampling rate, for both speaker and microphone.
     */
    Mono16K(1),

    /**
     * The mono audio mode with single audio channel and 48KHz sampling rate, for both speaker and microphone.
     */
    Mono48K(2),

    /**
     * The stereo audio mode with two audio channels for speaker, and single audio channel for microphone,
     * both with 48KHz sampling rate.
     */
    Stereo48K(3),

    /**
     * The [NoDevice] audio mode is obsolete, and is replaced by [AudioDeviceCapabilities.None]. To achieve the
     * same functionality as NoDevice, pass AudioDeviceCapabilities.None into the AudioVideoConfiguration constructor
     * instead, e.g. AudioVideoConfiguration(audioDeviceCapabilities = AudioDeviceCapabilities.None)
     */
    @Deprecated("To achieve the same functionality as NoDevice, pass AudioDeviceCapabilities.None into" +
            " the AudioVideoConfiguration constructor instead, e.g." +
            " AudioVideoConfiguration(audioDeviceCapabilities = AudioDeviceCapabilities.None)", level = DeprecationLevel.HIDDEN)
    NoDevice(4);

    companion object {
        fun from(intValue: Int): AudioMode? {
            if (intValue == 4) {
                // NoDevice cannot be instantiated since it is obsolete
                return null
            }
            return values().find { it.value == intValue }
        }

        fun from(intValue: Int, defaultAudioMode: AudioMode): AudioMode = from(intValue) ?: defaultAudioMode
    }
}
