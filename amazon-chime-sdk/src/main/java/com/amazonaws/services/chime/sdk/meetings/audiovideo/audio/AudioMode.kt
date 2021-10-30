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
     * There will be no audio through mic and speaker.
     */
    NoAudio(0),

    /**
     * The default audio mode with single audio channel.
     */
    Mono(1);

    companion object {
        fun from(intValue: Int): AudioMode? = values().find { it.value == intValue }
        fun from(intValue: Int, defaultAudioMode: AudioMode): AudioMode = from(intValue) ?: defaultAudioMode
    }
}
