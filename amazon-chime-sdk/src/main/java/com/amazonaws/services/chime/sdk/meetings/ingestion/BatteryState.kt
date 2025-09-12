/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * Enum representing different battery states
 */
enum class BatteryState(val description: String) {
    /** Battery is currently being charged by a power source */
    CHARGING("Charging"),

    /** Battery is currently being used and losing charge */
    DISCHARGING("Discharging"),

    /** Battery is connected to power but not actively charging (e.g., battery is full or charging is paused) */
    NOT_CHARGING("NotCharging"),

    /** Battery is at maximum capacity (100% charged) */
    FULL("Full"),

    /** Battery state cannot be determined or is not available */
    UNKNOWN("Unknown")
}
