/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

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
    private lateinit var eventClientConfiguration: EventClientConfiguration

    @MockK
    private lateinit var eventBuffer: EventBuffer

    @MockK
    private lateinit var timer: Timer

    @MockK
    private lateinit var logger: Logger

    private val flushIntervalMs = 200L
    private val metadataAttributeKey1 = "metadataAttributeKey1"
    private val metadataAttributeValue1 = 1
    private val metadataAttributeKey2 = "metadataAttributeKey2"
    private val metadataAttributeValue2 = "text"
    private val metadataAttributes = mapOf(
        metadataAttributeKey1 to metadataAttributeValue1,
        metadataAttributeKey2 to metadataAttributeValue2
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { ingestionConfiguration.disabled } returns false
        every { ingestionConfiguration.flushIntervalMs } returns flushIntervalMs
        every { ingestionConfiguration.clientConfiguration } returns eventClientConfiguration
        every { eventClientConfiguration.metadataAttributes } returns metadataAttributes
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
        DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger,
            timer
        )

        verify(exactly = 1) { timer.scheduleAtFixedRate(any(), flushIntervalMs, flushIntervalMs) }
    }

    @Test
    fun `report should add configuration common attributes`() {
        val defaultEventReporter = DefaultEventReporter(
            ingestionConfiguration,
            eventBuffer,
            logger
        )

        defaultEventReporter.report(SDKEvent(EventName.meetingFailed, mutableMapOf()))

        verify(exactly = 1) { eventBuffer.add(match {
            it.eventAttributes[metadataAttributeKey1] == metadataAttributeValue1 &&
                    it.eventAttributes[metadataAttributeKey2] == metadataAttributeValue2 }
        ) }
    }
}
