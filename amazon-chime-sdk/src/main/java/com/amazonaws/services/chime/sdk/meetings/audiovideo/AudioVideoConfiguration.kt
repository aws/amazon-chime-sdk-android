/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioRecordingPresetOverride
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioStreamType

/**
 * [AudioVideoConfiguration] represents the configuration to be used for audio and video during a
 * meeting session.
 *
 * @property audioMode: AudioMode - the audio mode in which the audio client should operate during
 * a meeting session.
 *
 * @property audioStreamType: AudioStreamType - the audio stream type in which the audio client
 * should operate during a meeting session.
 */
data class AudioVideoConfiguration @JvmOverloads constructor(
    val audioMode: AudioMode = AudioMode.Stereo48K,
    val audioStreamType: AudioStreamType = AudioStreamType.VoiceCall,
    val audioRecordingPresetOverride: AudioRecordingPresetOverride = AudioRecordingPresetOverride.Generic
)
