/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.enums

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
