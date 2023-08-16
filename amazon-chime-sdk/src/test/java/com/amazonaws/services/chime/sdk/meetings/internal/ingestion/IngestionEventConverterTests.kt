package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.ingestion.EventClientConfiguration
import com.amazonaws.services.chime.sdk.meetings.ingestion.EventClientType
import com.amazonaws.services.chime.sdk.meetings.ingestion.IngestionConfiguration
import com.amazonaws.services.chime.sdk.meetings.internal.utils.EventAttributesUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IngestionEventConverterTests {
    private class CustomEventClientConfiguration(
        override val type: EventClientType = EventClientType.Meet,
        override val eventClientJoinToken: String = "token",
        override val tag: String = "custom-tag",
        override val metadataAttributes: Map<String, Any> = mapOf(
            "meetingId" to "meeting1",
            "attendeeId" to "attendee1",
            "additionMetadata" to "value1"
        )
    ) : EventClientConfiguration

    private val ingestionConfiguration = IngestionConfiguration(
        clientConfiguration = CustomEventClientConfiguration(),
        ingestionUrl = "ingestion-url"
    )
    private val eventMeeting1 = SDKEvent(
        name = "some-event-name",
        eventAttributes = mapOf(
            "key-1" to "value1",
            "timestampMs" to 1.00,
            "meetingId" to "meeting1",
            "attendeeId" to "attendee1",
            "additionMetadata" to "value1"
        )
    )
    private val eventMeeting2 = SDKEvent(
        name = "some-event-name",
        eventAttributes = mapOf(
            "key-1" to "value1",
            "timestampMs" to 2.00,
            "meetingId" to "meeting2",
            "attendeeId" to "attendee2",
            "additionMetadata" to "value2"
        )
    )
    private val eventItem1 = MeetingEventItem(id = "event-id-1", data = eventMeeting1)
    private val eventItem2 = MeetingEventItem(id = "event-id-2", data = eventMeeting2)

    @Before
    fun setUp() {
        mockkObject(EventAttributesUtils)
    }

    @After
    fun cleanUp() {
        unmockkObject(EventAttributesUtils)
    }

    @Test
    fun `fromMeetingEventItems should create correct IngestionRecord`() {
        every { EventAttributesUtils.getIngestionMetadata(any()) } returns mapOf(
            "meetingId" to "meeting1", "attendeeId" to "attendee1",
            "additionMetadata" to "value1", "other-metadata" to "value"
        )

        val record = IngestionEventConverter.fromMeetingEventItems(
            listOf(eventItem1, eventItem2), ingestionConfiguration)

        // Event payload doesn't contain configuration metadata attributes
        for (event in record.events) {
            assertTrue(ingestionConfiguration.clientConfiguration.metadataAttributes.keys.none { metadataAttribute ->
                event.payloads.none { payload -> payload.containsKey(metadataAttribute) }
            })
        }
    }
}
