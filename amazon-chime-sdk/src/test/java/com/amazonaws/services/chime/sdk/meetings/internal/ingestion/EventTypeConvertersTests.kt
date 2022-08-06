package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EventTypeConvertersTests {

    private lateinit var eventTypeConverters: EventTypeConverters

    @MockK
    private lateinit var logger: Logger

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        eventTypeConverters = EventTypeConverters(logger)
    }

    @Test
    fun `toMeetingEvent should convert json string to object`() {

        val nameValue = "test name"
        val deviceNameKey = EventAttributeName.deviceName.toString()
        val deviceNameValue = "test device"
        val tsKey = EventAttributeName.timestampMs.toString()
        val tsValue = 1659759236803
        val data = "{\"name\":\"$nameValue\", \"eventAttributes\":{\"$deviceNameKey\":\"$deviceNameValue\", \"$tsKey\":$tsValue}}"
        val event = eventTypeConverters.toMeetingEvent(data)

        assertEquals(event.name, nameValue)
        assertEquals(event.eventAttributes[EventAttributeName.deviceName] as? String, deviceNameValue)
        assertEquals((event.eventAttributes[EventAttributeName.timestampMs] as? Double ?: 0.0).toLong(), tsValue)
    }

    @Test
    fun `toMeetingEvent should handle null name`() {

        val data = "{\"name\":null, \"eventAttributes\":{}}"
        val event = eventTypeConverters.toMeetingEvent(data)

        assertEquals(event.name, "")
    }

    @Test
    fun `toMeetingEvent should handle malformed name`() {

        val data = "{\"name\":{}, \"eventAttributes\":{}}"
        val event = eventTypeConverters.toMeetingEvent(data)

        verify(exactly = 1) { logger.error(any(), any()) }

        assertEquals(event.name, "")
    }

    @Test
    fun `toMeetingEvent should handle null event attributes data`() {

        val nameValue = "test name"
        val data = "{\"name\":\"$nameValue\", \"eventAttributes\":null}"
        val event = eventTypeConverters.toMeetingEvent(data)

        assertEquals(event.eventAttributes.count(), 0)
    }

    @Test
    fun `toMeetingEvent should handle malformed event attributes data`() {

        val nameValue = "test name"
        val data = "{\"name\":\"$nameValue\", \"eventAttributes\":\"invalid data\"}"
        val event = eventTypeConverters.toMeetingEvent(data)

        verify(exactly = 1) { logger.error(any(), any()) }

        assertEquals(event.eventAttributes.count(), 0)
    }

    @Test
    fun `fromMeetingEvent should convert object to json string`() {

        val nameValue = "test name"
        val deviceNameKey = EventAttributeName.deviceName.toString()
        val deviceNameValue = "test device"
        val tsKey = EventAttributeName.timestampMs.toString()
        val tsValue = 1659759236803
        val mockEvent = SDKEvent(nameValue, mutableMapOf(
            EventAttributeName.deviceName to deviceNameValue,
            EventAttributeName.timestampMs to tsValue
        ))
        val result = eventTypeConverters.fromMeetingEvent(mockEvent)

        val jsonString = "{\"name\":\"test name\",\"eventAttributes\":{\"$deviceNameKey\":\"$deviceNameValue\",\"$tsKey\":$tsValue}}"
        assertEquals(result, jsonString)
    }
}
