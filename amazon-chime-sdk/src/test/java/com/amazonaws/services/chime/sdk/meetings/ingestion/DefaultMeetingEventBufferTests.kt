/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyEventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.DirtyMeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.EventDao
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.IngestionEventConverter
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.MeetingEventItem
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultMeetingEventBufferTests {
    @MockK
    private lateinit var ingestionConfiguration: IngestionConfiguration

    @MockK
    private lateinit var eventDao: EventDao

    @MockK
    private lateinit var dirtyEventDao: DirtyEventDao

    @MockK
    private lateinit var eventSender: EventSender

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var calendar: Calendar

    @ExperimentalCoroutinesApi
    private val eventScope: CoroutineScope = TestCoroutineScope()

    private lateinit var defaultMeetingEventBuffer: EventBuffer

    private val id = "id"
    private val attendeeId = "attendeeId"
    private val meetingId = "meetingId"
    private val meetingEvent = SDKEvent(EventName.meetingFailed, mutableMapOf())
    private val meetingEventItem = MeetingEventItem(id, meetingEvent)
    private val dirtyMeetingEventItem = DirtyMeetingEventItem(id, meetingEvent, 1214124L)
    private val ingestionRecord = IngestionRecord(
        mutableMapOf(), listOf(
            IngestionEvent(
                EventClientType.Meet, mutableMapOf(), listOf(
                    mutableMapOf(
                        "status" to "ok",
                        "id" to id
                    )
                )
            )
        )
    )
    private val flushSize = 5

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockkObject(IngestionEventConverter)
        every { ingestionConfiguration.flushSize } returns flushSize
        every { calendar.timeInMillis } returns 100L
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        every { ingestionConfiguration.clientConfiguration } returns MeetingEventClientConfiguration(
            "joinToken",
            meetingId,
            attendeeId
        )

        defaultMeetingEventBuffer = DefaultMeetingEventBuffer(
            ingestionConfiguration,
            eventDao,
            dirtyEventDao,
            eventSender,
            logger,
            eventScope
        )
    }

    @Test
    fun `defaultMeetingEventBuffer should process expired dirtyEvents if send failed`() {
        val expiredEventId = "newId"
        val ttl = 100L
        coEvery { eventSender.sendRecord(any()) } returns false
        every { calendar.timeInMillis } returns 101L
        every { IngestionEventConverter.fromDirtyMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )
        every { dirtyEventDao.listDirtyMeetingEventItems(any()) } returns listOf(
            dirtyMeetingEventItem,
            DirtyMeetingEventItem(expiredEventId, meetingEvent, ttl)
        )

        DefaultMeetingEventBuffer(
            ingestionConfiguration,
            eventDao,
            dirtyEventDao,
            eventSender,
            logger,
            eventScope
        )

        runBlockingTest {
            verify { dirtyEventDao.deleteDirtyEventsByIds(listOf(expiredEventId)) }
        }
    }

    @Test
    fun `add should invoke EventDao insertMeetingEvent`() {
        defaultMeetingEventBuffer.add(meetingEvent)

        verify(exactly = 1) { eventDao.insertMeetingEvent(any()) }
    }

    @Test
    fun `add should send immediately if it is meeting failed`() {
        coEvery { eventSender.sendRecord(any()) } returns true
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )

        val currentMeetingEvent = SDKEvent(
            EventName.meetingFailed, mapOf(
                EventAttributeName.meetingStatus to MeetingSessionStatusCode.AudioInternalServerError
            ) as EventAttributes
        )
        defaultMeetingEventBuffer.add(currentMeetingEvent)

        runBlockingTest {
            verify(exactly = 1) { eventDao.insertMeetingEvent(any()) }
            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `add should not send immediately if it is not meeting failed`() {
        coEvery { eventSender.sendRecord(any()) } returns true
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )

        val currentMeetingEvent = SDKEvent(
            EventName.meetingStartSucceeded, mapOf(
                EventAttributeName.meetingStatus to MeetingSessionStatusCode.OK
            ) as EventAttributes
        )
        defaultMeetingEventBuffer.add(currentMeetingEvent)

        runBlockingTest {
            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `process should not invoke eventSender sendRecord when there is no stored events`() {
        every { eventDao.listMeetingEventItems(any()) } returns emptyList()

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `process should invoke eventSender sendRecord when there is stored events`() {
        every { eventDao.listMeetingEventItems(any()) } returns listOf(meetingEventItem)
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )
        coEvery { eventSender.sendRecord(any()) } returns true

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
            verify(exactly = 1) { eventDao.listMeetingEventItems(any()) }
            verify(exactly = 1) { eventDao.deleteMeetingEventsByIds(any()) }
        }
    }

    @Test
    fun `process should remove processed ids when sent succeeded`() {
        every { eventDao.listMeetingEventItems(any()) } returns listOf(meetingEventItem)
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )
        coEvery { eventSender.sendRecord(any()) } returns true

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            verify(exactly = 1) { eventDao.deleteMeetingEventsByIds(listOf(id)) }
        }
    }

    @Test
    fun `process should remove processed ids when sent failed`() {
        every { eventDao.listMeetingEventItems(any()) } returns listOf(meetingEventItem)
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )
        coEvery { eventSender.sendRecord(any()) } returns false

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            verify(exactly = 1) { eventDao.deleteMeetingEventsByIds(listOf(id)) }
        }
    }

    @Test
    fun `process should insert events as dirtyEvents when failed to send`() {
        every { eventDao.listMeetingEventItems(any()) } returns listOf(meetingEventItem)
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) }.returns(
            ingestionRecord
        )
        coEvery { eventSender.sendRecord(any()) } returns false

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
            verify(exactly = 1) { eventDao.deleteMeetingEventsByIds(any()) }
            verify(exactly = 1) { dirtyEventDao.insertDirtyMeetingEventItems(any()) }
        }
    }
}
