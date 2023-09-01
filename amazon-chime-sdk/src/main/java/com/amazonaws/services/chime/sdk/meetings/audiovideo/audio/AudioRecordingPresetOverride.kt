/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

/**
 * [AudioRecordingPresetOverride] describes the audio recording preset in which
 * the audio client should operate during a meeting session.
 * The values below (except None) directly map to the values defined in:
 * https://android.googlesource.com/platform/frameworks/wilhelm/+/master/include/SLES/OpenSLES_AndroidConfiguration.h
 */
enum class AudioRecordingPresetOverride {

    /**
     * No preset override. We will use default preset depending on the mode of operation
     */
    None,

    /**
     * Equivalent of SL_ANDROID_RECORDING_PRESET_GENERIC in openSLES
     */
    Generic,

    /**
     * Equivalent of SL_ANDROID_RECORDING_PRESET_CAMCORDER in openSLES
     */
    Camcorder,

    /**
     * Equivalent of SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION in openSLES
     */
    VoiceRecognition,

    /**
     * Equivalent of SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION in openSLES
     */
    VoiceCommunication
}
