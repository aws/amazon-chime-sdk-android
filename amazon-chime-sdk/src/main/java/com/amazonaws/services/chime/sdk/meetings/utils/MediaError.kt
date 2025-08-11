/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

// Enum class representing different media-related errors
enum class MediaError {
    /**
     * Error indicating failure to set the audio route (e.g., switching between speakers, headphones)
     */
    FailedToSetRoute,

    /**
     * Error indicating that no audio input/output devices are available or detected
     */
    NoAudioDevices;
}
