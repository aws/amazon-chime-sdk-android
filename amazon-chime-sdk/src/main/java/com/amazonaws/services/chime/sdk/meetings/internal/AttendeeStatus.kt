/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal

/**
 * [AttendeeStatus] describes status of Attendee - Join, Leave, Drop
 */
enum class AttendeeStatus(val value: Int) {
    /**
     * The attendee joined
     */
    Joined(1),

    /**
     * The attendee left
     */
    Left(2),

    /**
     * The attendee dropped due to network
     */
    Dropped(3);

    companion object {
        fun from(intValue: Int): AttendeeStatus? = values().find { it.value == intValue }
    }
}
