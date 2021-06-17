/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.Timer
import org.junit.Before
import org.junit.Test

class DefaultEventReporterTests {
    @MockK
    private lateinit var ingestionConfiguration: IngestionConfiguration

    @MockK
    private lateinit var eventBuffer: EventBuffer

    @MockK
    private lateinit var timer: Timer

    @MockK
    private lateinit var logger: Logger

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { ingestionConfiguration.flushSize } returns 20
        every { ingestionConfiguration.flushIntervalMs } returns 200
    }

    @Test
    fun `constructor should not invoke timer schedule if disabled`() {
        every { ingestionConfiguration.disabled } returns true

        DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger
        )

        verify(exactly = 0) { timer.scheduleAtFixedRate(any(), any<Long>(), any()) }
        verify(exactly = 0) { eventBuffer.process() }
    }

    @Test
    fun `constructor should invoke timer schedule if enabled`() {
        every { ingestionConfiguration.disabled } returns false

        DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger,
            timer
        )

        verify(exactly = 1) { timer.scheduleAtFixedRate(any(), any<Long>(), any()) }
    }

    @Test
    fun `report should call eventBuffer add`() {
        val meetingId = "meetingId"
        val attendeeId = "attendeeId"
        val joinToken = ""
        every { ingestionConfiguration.disabled } returns false
        every { ingestionConfiguration.clientConfiguration } returns MeetingEventClientConfiguration(joinToken, meetingId, attendeeId)

        val defaultEventReporter = DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger
        )
        defaultEventReporter.report(SDKEvent(EventName.meetingFailed, mutableMapOf()))

        verify(exactly = 1) { eventBuffer.add(match {
            it.eventAttributes[EventAttributeName.meetingId] == meetingId &&
                    it.eventAttributes[EventAttributeName.attendeeId] == attendeeId }
        ) }
    }
}
