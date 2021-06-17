/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database

import android.content.ContentValues
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.EventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.EventTypeConverters
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.MeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class EventSQLiteDao(
    private val databaseManager: DatabaseManager,
    private val logger: Logger
) : EventDao, DatabaseTable {
    override val tableName = "Events"
    override val columns: Map<String, String>
        get() = mapOf(
            dataColumnName to dataColumnType
        )
    override val primaryKey: Pair<String, String>
        get() = (idColumnName to idColumnType)
    private val TAG = "EventSQLiteDao"

    private val idColumnName = "id"
    private val dataColumnName = "data"
    private val idColumnType = "TEXT"
    private val dataColumnType = "TEXT NOT NULL"

    init {
        databaseManager.createTable(this)
    }

    override fun listMeetingEventItems(size: Int): List<MeetingEventItem> {
        val retrievedDataList = databaseManager.query(tableName, size)

        return retrievedDataList.map { retrievedData ->
            MeetingEventItem(
                retrievedData[idColumnName] as String,
                EventTypeConverters.toMeetingEvent(retrievedData[dataColumnName] as String)
            )
        }
    }

    override fun insertMeetingEvent(event: MeetingEventItem): Boolean {
        val values = ContentValues().apply {
            put(idColumnName, event.id)
            put(dataColumnName, EventTypeConverters.fromMeetingEvent(event.data))
        }

        return databaseManager.insert(tableName, listOf(values))
    }

    override fun deleteMeetingEventsByIds(ids: List<String>): Int {
        return databaseManager.delete(tableName, idColumnName, ids)
    }
}
