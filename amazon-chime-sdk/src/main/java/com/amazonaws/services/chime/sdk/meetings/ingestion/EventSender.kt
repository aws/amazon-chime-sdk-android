/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

/**
 * [EventSender] is responsible for sending [IngestionRecord].
 */
interface EventSender {
    /**
     * Send ingestion record.
     *
     * @param record: [IngestionRecord] - record to send
     * @return whether sending was successful or not.
     */
    suspend fun sendRecord(record: IngestionRecord): Boolean
}
