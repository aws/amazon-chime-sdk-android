/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * [NoopEventReporterFactory] returns null [EventReporter]
 */
class NoopEventReporterFactory : EventReporterFactory {
    override fun createEventReporter(): EventReporter? {
        return null
    }
}
