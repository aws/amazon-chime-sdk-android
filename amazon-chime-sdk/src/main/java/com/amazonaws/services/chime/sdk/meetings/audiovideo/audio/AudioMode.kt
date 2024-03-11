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

    @Deprecated("Use AudioDeviceCapabilities.None instead", level = DeprecationLevel.HIDDEN)
    NoDevice(4);

    companion object {
        // Return null for value 4 since NoDevice should not be accessible from this function
        fun from(intValue: Int): AudioMode? = if (intValue == 4) null else values().find { it.value == intValue }
        fun from(intValue: Int, defaultAudioMode: AudioMode): AudioMode = from(intValue) ?: defaultAudioMode
    }
}
