/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * [EventReporterFactory] facilitates creating [EventReporter]
 */
interface EventReporterFactory {
    /**
     * Create [EventReporter] and return null if no-op is needed on event reporting.
     *
     * @return [EventReporter] - event reporter created.
     */
    fun createEventReporter(): EventReporter?
}
