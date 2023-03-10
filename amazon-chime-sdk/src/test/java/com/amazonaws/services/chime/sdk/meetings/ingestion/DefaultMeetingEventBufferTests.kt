/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

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

    @MockK
    private lateinit var ingestionRecord: IngestionRecord

    @ExperimentalCoroutinesApi
    private val eventScope: CoroutineScope = TestCoroutineScope()

    private lateinit var defaultMeetingEventBuffer: EventBuffer

    private val currentTimestampMs = 100L
    private val futureTtl = 101L
    private val expiredTtl = 99L
    private val expiredDirtyEventId = "expired-event-id"
    private val malformedDirtyEventId = "malformed-event-id"
    private val validSDKEvent = SDKEvent(name = "event-name", eventAttributes = mapOf("timestampMs" to 1000))
    private val malformedSDKEvent = SDKEvent(name = "event-name", eventAttributes = mapOf("blah" to 1000))
    private val expiredDirtyEventItem = DirtyMeetingEventItem(id = expiredDirtyEventId, data = validSDKEvent, ttl = expiredTtl)
    private val malformedDirtyEventItem = DirtyMeetingEventItem(id = malformedDirtyEventId, data = malformedSDKEvent, ttl = futureTtl)
    private val event = SDKEvent(name = "non-urgent-event", eventAttributes = emptyMap())
    private val urgentEvent = SDKEvent(name = "meetingFailed", eventAttributes = mapOf("meetingStatus" to MeetingSessionStatusCode.AudioAuthenticationRejected))
    private val eventItem = MeetingEventItem(id = "event-id", data = event)
    private val flushSize = 5

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockkObject(IngestionEventConverter)
        every { ingestionConfiguration.flushSize } returns flushSize
        every { calendar.timeInMillis } returns currentTimestampMs
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        every { IngestionEventConverter.fromDirtyMeetingEventItems(any(), any()) } returns ingestionRecord
        every { IngestionEventConverter.fromMeetingEventItems(any(), any()) } returns ingestionRecord

        every { dirtyEventDao.listDirtyMeetingEventItems(any()) } returns emptyList()
        coEvery { eventSender.sendRecord(any()) } returns true
    }

    private fun construct() {
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
    fun `should delete expired events when process the dirty events`() {
        every { dirtyEventDao.listDirtyMeetingEventItems(any()) } returns
                listOf(expiredDirtyEventItem)

        construct()

        runBlockingTest {
            verify(exactly = 1) { dirtyEventDao.deleteDirtyEventsByIds(listOf(expiredDirtyEventId)) }
            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `should delete malformed events when process the dirty events`() {
        every { dirtyEventDao.listDirtyMeetingEventItems(any()) } returns
                listOf(malformedDirtyEventItem)

        construct()

        runBlockingTest {
            verify(exactly = 1) { dirtyEventDao.deleteDirtyEventsByIds(listOf(malformedDirtyEventId)) }
            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `add should insert event to EventDao`() {
        construct()

        defaultMeetingEventBuffer.add(event)

        verify(exactly = 1) { eventDao.insertMeetingEvent(any()) }
    }

    @Test
    fun `add should send immediately if it is urgent event`() {
        construct()

        defaultMeetingEventBuffer.add(urgentEvent)

        runBlockingTest {
            verify(exactly = 1) { eventDao.insertMeetingEvent(any()) }
            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `add should not send immediately if it is not urgent event`() {
        construct()

        defaultMeetingEventBuffer.add(event)

        runBlockingTest {
            verify(exactly = 1) { eventDao.insertMeetingEvent(any()) }
            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `process should not send events when there is no stored events`() {
        construct()
        every { eventDao.listMeetingEventItems(any()) } returns emptyList()

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 0) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `process should send events when there is stored events`() {
        construct()
        every { eventDao.listMeetingEventItems(any()) } returns listOf(eventItem)

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
        }
    }

    @Test
    fun `process should remove processed event items`() {
        construct()
        every { eventDao.listMeetingEventItems(any()) } returns listOf(eventItem)

        runBlockingTest {
            // send successfully
            coEvery { eventSender.sendRecord(any()) } returns true
            defaultMeetingEventBuffer.process()
            // send failed
            coEvery { eventSender.sendRecord(any()) } returns false
            defaultMeetingEventBuffer.process()

            verify(exactly = 2) { eventDao.deleteMeetingEventsByIds(listOf(eventItem.id)) }
        }
    }

    @Test
    fun `process should insert events as dirtyEvents when failed to send`() {
        construct()
        every { eventDao.listMeetingEventItems(any()) } returns listOf(eventItem)
        coEvery { eventSender.sendRecord(any()) } returns false

        runBlockingTest {
            defaultMeetingEventBuffer.process()

            coVerify(exactly = 1) { eventSender.sendRecord(any()) }
            verify(exactly = 1) { eventDao.deleteMeetingEventsByIds(any()) }
            verify(exactly = 1) { dirtyEventDao.insertDirtyMeetingEventItems(any()) }
        }
    }
}
