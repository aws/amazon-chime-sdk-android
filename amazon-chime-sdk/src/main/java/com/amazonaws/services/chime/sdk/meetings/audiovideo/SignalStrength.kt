/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

/**
 * [SignalStrength] describes the signal strength of an attendee for audio
 */
enum class SignalStrength(val value: Int) {
    /**
     * The attendee has no signal
     */
    None(0),

    /**
     * The attendee has low signal
     */
    Low(1),

    /**
     * The attendee has high signal
     */
    High(2);

    companion object {
        fun from(intValue: Int): SignalStrength? = values().find { it.value == intValue }
    }
}
