/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * Interface for monitoring application state changes
 */
interface AppStateMonitor {
    /**
     * The current application state
     */
    val appState: AppState

    /**
     * Bind a handler to handle app state events triggered by state changes.
     *
     * @param handler Handler for app state events
     */
    fun bindHandler(handler: AppStateHandler)

    /**
     * Start monitoring application state changes
     */
    fun start()

    /**
     * Stop monitoring application state changes
     */
    fun stop()

    /**
     * Returns the current battery level as a value between 0.0 and 1.0
     * @return Battery level (0.0 = empty, 1.0 = full), or null if unable to determine
     */
    fun getBatteryLevel(): Float?

    /**
     * Returns the current battery state
     * @return Battery state
     */
    fun getBatteryState(): BatteryState

    /**
     * Checks whether Android's Battery Saver mode (a.k.a. Low Power Mode) is currently enabled.
     *
     * @return true if Battery Saver mode is on, false otherwise.
     */
    fun isBatterySaverOn(): Boolean
}
