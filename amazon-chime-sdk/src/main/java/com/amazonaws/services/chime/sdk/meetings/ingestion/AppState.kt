/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * Represents the current state of the application
 */
enum class AppState(val description: String) {
    /**
     * The application is in the foreground and receiving events
     */
    ACTIVE("Active"),

    /**
     * The application is in the foreground but not receiving events
     * (e.g., when an incoming phone call or SMS message arrives)
     */
    INACTIVE("Inactive"),

    /**
     * The application is running in the background
     */
    BACKGROUND("Background"),

    /**
     * The application is in the foreground (covers both active and inactive states)
     */
    FOREGROUND("Foreground")
}
