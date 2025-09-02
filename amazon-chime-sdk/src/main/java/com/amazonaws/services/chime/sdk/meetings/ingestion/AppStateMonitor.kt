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
}
