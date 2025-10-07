/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * Allows a component to respond to app state events from [AppStateMonitor].
 */
interface AppStateHandler {
    /**
     * Called when the application state changes
     *
     * @param newAppState The new application state
     */
    fun onAppStateChanged(newAppState: AppState)

    /**
     * Called when the application receives a memory warning
     */
    fun onMemoryWarning()

    /**
     * Called when the network connection type is changed
     */
    fun onNetworkConnectionTypeChanged(type: NetworkConnectionType)
}
