/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent

/**
 * [EventReporter] is class that process meeting event that is created in [EventAnalyticsController].
 */
interface EventReporter {
    /**
     * Report the meeting event
     *
     * @param event: [SDKEvent] - Event that has name and attributes associated.
     */
    fun report(event: SDKEvent)

    /**
     * Start [EventReporter] and process data
     */
    fun start()

    /**
     * Stop [EventReporter] and processing data
     */
    fun stop()
}
