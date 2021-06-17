/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.DatabaseManager
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.DirtyEventSQLiteDao
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.UUID
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DirtyEventSQLiteDaoTests {
    private val tableName = "DirtyEvents"
    private val gson = Gson()
    private lateinit var dirtyEventDao: DirtyEventSQLiteDao

    @MockK
    private lateinit var databaseManager: DatabaseManager

    @MockK
    private lateinit var logger: Logger

    private val uuid = "38400000-8cf0-11bd-b23e-10b96e4ef00d"

    private val mockEvent = SDKEvent(
        EventName.meetingFailed, mutableMapOf(
            EventAttributeName.meetingErrorMessage to "test fail",
            EventAttributeName.meetingStatus to MeetingSessionStatusCode.AudioAuthenticationRejected
        )
    )

    private fun buildMockMeetingEventItem(): DirtyMeetingEventItem =
        DirtyMeetingEventItem(UUID.randomUUID().toString(), mockEvent, 200000)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { databaseManager.dropTable(any()) } returns true
        every { databaseManager.createTable(any()) } returns true
        every { databaseManager.delete(any(), any(), any()) } returns 0
        every { databaseManager.query(any(), any()) } returns listOf(
            mapOf(
                "id" to "51ed7a0b-906c-4ab6-9b4b-2ba9a012105b",
                "data" to gson.toJson(mockEvent),
                "ttl" to 1242312412424
            )
        )

        dirtyEventDao = DirtyEventSQLiteDao(databaseManager, logger)
    }

    @Test
    fun `insertDirtyMeetingEventItems should invoke database manager insert`() {
        every { databaseManager.insert(any(), any()) } returns true

        dirtyEventDao.insertDirtyMeetingEventItems(listOf(buildMockMeetingEventItem()))

        verify(exactly = 1) { databaseManager.insert(tableName, any()) }
    }

    @Test
    fun `insertDirtyMeetingEventItems should return false if database manager insert fails`() {
        every { databaseManager.insert(any(), any()) } returns false

        val inserted = dirtyEventDao.insertDirtyMeetingEventItems(listOf(buildMockMeetingEventItem()))

        Assert.assertEquals(false, inserted)
    }

    @Test
    fun `queryDirtyMeetingEventItems should invoke database manager query`() {
        dirtyEventDao.listDirtyMeetingEventItems(5)

        verify(exactly = 1) { databaseManager.query(tableName, 5) }
    }

    @Test
    fun `queryDirtyMeetingEventItems should return a list of DirtyMeetingEventItems`() {
        val dirtyMeetingItems = dirtyEventDao.listDirtyMeetingEventItems(5)

        Assert.assertEquals(1, dirtyMeetingItems.size)
        Assert.assertEquals(mockEvent.name, dirtyMeetingItems[0].data.name)
        Assert.assertEquals(
            mockEvent.eventAttributes[EventAttributeName.meetingErrorMessage],
            dirtyMeetingItems[0].data.eventAttributes[EventAttributeName.meetingErrorMessage]
        )
    }

    @Test
    fun `deleteDirtyEventsByIds should invoke database manager delete`() {
        val uuids = listOf(uuid)
        dirtyEventDao.deleteDirtyEventsByIds(uuids)

        verify(exactly = 1) { databaseManager.delete(tableName, dirtyEventDao.primaryKey.first, uuids) }
    }

    @Test
    fun `deleteDirtyEventsByIds should return zero if database manager delete returns 0`() {
        val uuids = listOf(uuid)
        val rowsDeleted = dirtyEventDao.deleteDirtyEventsByIds(uuids)

        Assert.assertEquals(0, rowsDeleted)
    }

    @Test
    fun `constructor should invoke database manager createTable`() {
        dirtyEventDao = DirtyEventSQLiteDao(databaseManager, logger)

        verify { databaseManager.createTable(dirtyEventDao) }
    }
}
