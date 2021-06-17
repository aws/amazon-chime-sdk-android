/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.DatabaseManager
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.database.EventSQLiteDao
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

class EventSQLiteDaoTests {
    private val tableName = "Events"
    private lateinit var eventDao: EventSQLiteDao
    private val gson = Gson()

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

    private fun buildMockMeetingEventItem(): MeetingEventItem =
        MeetingEventItem(UUID.randomUUID().toString(), mockEvent)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { databaseManager.dropTable(any()) } returns true
        every { databaseManager.createTable(any()) } returns true
        every { databaseManager.delete(any(), any(), any()) } returns 0
        every { databaseManager.query(any(), any()) } returns listOf(
            mapOf(
                "id" to "51ed7a0b-906c-4ab6-9b4b-2ba9a012105b",
                "data" to gson.toJson(mockEvent)
            )
        )

        eventDao =
            EventSQLiteDao(
                databaseManager,
                logger
            )
    }

    @Test
    fun `insertMeetingEvent should invoke database manager insert`() {
        every { databaseManager.insert(any(), any()) } returns true

        eventDao.insertMeetingEvent(buildMockMeetingEventItem())

        verify(exactly = 1) { databaseManager.insert(tableName, any()) }
    }

    @Test
    fun `queryMeetingEventItems should invoke database manager query`() {
        eventDao.listMeetingEventItems(5)

        verify(exactly = 1) { databaseManager.query(tableName, 5) }
    }

    @Test
    fun `queryMeetingEventItems should return a list of MeetingEventItem`() {
        val meetingItems = eventDao.listMeetingEventItems(5)

        Assert.assertEquals(1, meetingItems.size)
        Assert.assertEquals(
            mockEvent.eventAttributes[EventAttributeName.meetingErrorMessage],
            meetingItems[0].data.eventAttributes[EventAttributeName.meetingErrorMessage]
        )
    }

    @Test
    fun `deleteEventsByIds should invoke database manager delete`() {
        val uuids = listOf(uuid)
        eventDao.deleteMeetingEventsByIds(uuids)

        verify(exactly = 1) { databaseManager.delete(tableName, eventDao.primaryKey.first, uuids) }
    }

    @Test
    fun `deleteEventsByIds should return zero if database manager delete returns 0`() {
        val uuids = listOf(uuid)
        val rowsDeleted = eventDao.deleteMeetingEventsByIds(uuids)

        Assert.assertEquals(0, rowsDeleted)
    }

    @Test
    fun `constructor should invoke database manager createTable`() {
        eventDao =
            EventSQLiteDao(
                databaseManager,
                logger
            )

        verify { databaseManager.createTable(eventDao) }
    }
}
