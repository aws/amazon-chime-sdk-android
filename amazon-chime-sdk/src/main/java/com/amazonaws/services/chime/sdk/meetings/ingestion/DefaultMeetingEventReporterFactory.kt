/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.DirtyEventSQLiteDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.EventSQLiteDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.SQLiteDatabaseManager
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultMeetingEventReporterFactory(
    private val context: Context,
    private val ingestionConfiguration: IngestionConfiguration,
    private val logger: Logger
) : EventReporterFactory {
    override fun createEventReporter(): EventReporter? {
        if (ingestionConfiguration.disabled) {
            return null
        }
        val eventSender = DefaultEventSender(
            ingestionConfiguration,
            logger
        )
        val sqliteManager =
            SQLiteDatabaseManager(
                context,
                logger
            )
        val eventDao =
            EventSQLiteDao(
                sqliteManager,
                logger
            )
        val dirtyEventDao =
            DirtyEventSQLiteDao(
                sqliteManager,
                logger
            )

        val eventBuffer = DefaultMeetingEventBuffer(
            ingestionConfiguration,
            eventDao,
            dirtyEventDao,
            eventSender,
            logger
        )
        return DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger
        )
    }
}
