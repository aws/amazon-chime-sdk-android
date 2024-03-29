/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

/**
 * [AudioStreamType] describes the audio stream type in which the audio client should operate
 * during a meeting session.
 */
enum class AudioStreamType {

    /**
     * Equivalent of AudioManager.STREAM_VOICE_CALL
     * https://developer.android.com/reference/android/media/AudioManager#STREAM_VOICE_CALL
     */
    VoiceCall,

    /**
     * Equivalent of AudioManager.STREAM_MUSIC
     * https://developer.android.com/reference/android/media/AudioManager#STREAM_MUSIC
     * Note that the meeting session volume cannot be adjusted by the volume button with [Music]
     * except for Oculus Quest 2.
     */
    Music
}
