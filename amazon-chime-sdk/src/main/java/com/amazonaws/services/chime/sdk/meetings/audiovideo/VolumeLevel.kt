/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

/**
 * [VolumeLevel] describes the volume level of an attendee for audio
 */
enum class VolumeLevel(val value: Int) {
    /**
     * The attendee is muted
     */
    Muted(-1),

    /**
     * The attendee is not speaking
     */
    NotSpeaking(0),

    /**
     * The attendee is speaking at low volume
     */
    Low(1),

    /**
     * The attendee is speaking at medium volume
     */
    Medium(2),

    /**
     * The attendee is speaking at high volume
     */
    High(3);

    companion object {
        fun from(intValue: Int): VolumeLevel? = values().find { it.value == intValue }
    }
}
