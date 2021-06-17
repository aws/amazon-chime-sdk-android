/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database

import android.content.ContentValues
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyEventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyMeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.EventTypeConverters
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DirtyEventSQLiteDao(
    private val databaseManager: DatabaseManager,
    private val logger: Logger
) : DirtyEventDao, DatabaseTable {
    override val tableName = "DirtyEvents"
    override val columns: Map<String, String>
        get() = mapOf(dataColumnName to dataColumnType, ttlColumnName to ttlColumnType)
    override val primaryKey: Pair<String, String>
        get() = (idColumnName to idColumnType)

    private val TAG = "DirtyEventSQLiteDao"
    private val idColumnName = "id"
    private val dataColumnName = "data"
    private val ttlColumnName = "ttl"
    private val idColumnType = "TEXT"
    private val dataColumnType = "TEXT NOT NULL"
    private val ttlColumnType = "INTEGER NOT NULL"
    init {
        databaseManager.createTable(this)
    }

    override fun listDirtyMeetingEventItems(size: Int): List<DirtyMeetingEventItem> {
        val retrievedDataList = databaseManager.query(tableName, size)

        return retrievedDataList.map { retrievedData ->
            DirtyMeetingEventItem(
                retrievedData[idColumnName] as String,
                EventTypeConverters.toMeetingEvent(retrievedData[dataColumnName] as String),
                retrievedData[ttlColumnName] as Long
            )
        }
    }

    override fun deleteDirtyEventsByIds(ids: List<String>): Int {
        return databaseManager.delete(tableName, idColumnName, ids)
    }

    override fun insertDirtyMeetingEventItems(dirtyEvents: List<DirtyMeetingEventItem>): Boolean {
        return databaseManager.insert(tableName, dirtyEvents.map { dirtyEvent ->
            ContentValues().apply {
                put(idColumnName, dirtyEvent.id)
                put(dataColumnName, EventTypeConverters.fromMeetingEvent(dirtyEvent.data))
                put(ttlColumnName, dirtyEvent.ttl)
            }
        })
    }
}
