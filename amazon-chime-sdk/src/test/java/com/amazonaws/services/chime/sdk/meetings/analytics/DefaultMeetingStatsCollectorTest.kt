/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultMeetingStatsCollectorTest {
    @MockK
    private lateinit var mockLogger: Logger

    @InjectMockKs
    private lateinit var testMeetingStatsCollector: DefaultMeetingStatsCollector

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `maxVideoTileCount should take the higher value between the current maxVideoTileCount and the value passed in`() {
        testMeetingStatsCollector.updateMaxVideoTile(5)

        val maxVideoTileCount = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.maxVideoTileCount]
        assertEquals(5, maxVideoTileCount)

        testMeetingStatsCollector.updateMaxVideoTile(6)
        val maxVideoTileCountAfter = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.maxVideoTileCount]
        assertEquals(6, maxVideoTileCountAfter)

        testMeetingStatsCollector.updateMaxVideoTile(5)
        val maxVideoTileCountLast = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.maxVideoTileCount]
        assertEquals(6, maxVideoTileCountLast)
    }

    @Test
    fun `resetMeetingStats should reset meeting stats`() {
        testMeetingStatsCollector.updateMeetingStartTimeMs()
        testMeetingStatsCollector.incrementPoorConnectionCount()
        testMeetingStatsCollector.incrementRetryCount()
        testMeetingStatsCollector.updateMaxVideoTile(5)
        testMeetingStatsCollector.updateMeetingStartReconnectingTimeMs()
        Thread.sleep(1000)
        testMeetingStatsCollector.updateMeetingReconnectedTimeMs()
        testMeetingStatsCollector.resetMeetingStats()

        val meetingDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingDurationMs]
        val retryCount = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.retryCount]
        val poorConnectionCount = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.poorConnectionCount]
        val maxVideoTileCount = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.maxVideoTileCount]
        val meetingReconnectDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingReconnectDurationMs]
        assertEquals(0L, meetingDurationMs)
        assertEquals(0, retryCount)
        assertEquals(0, poorConnectionCount)
        assertEquals(0, maxVideoTileCount)
        assertEquals(0L, meetingReconnectDurationMs)
    }

    @Test
    fun `getMeetingHistory should return current meeting history`() {
        assertEquals(0, testMeetingStatsCollector.getMeetingHistory().size)

        testMeetingStatsCollector.addMeetingHistoryEvent(MeetingHistoryEventName.meetingStartSucceeded, 100)

        assertEquals(1, testMeetingStatsCollector.getMeetingHistory().size)
    }

    @Test
    fun `getMeetingStatsEventAttributes should return 0 if meetingStartReconnectingTimeMs is not set`() {
        val meetingReconnectDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingReconnectDurationMs]
        assertEquals(0L, meetingReconnectDurationMs)
    }

    @Test
    fun `getMeetingStatsEventAttributes should return 0 if meetingReconnectedTimeMs is not set`() {
        testMeetingStatsCollector.updateMeetingReconnectedTimeMs()
        val meetingReconnectDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingReconnectDurationMs]
        assertEquals(0L, meetingReconnectDurationMs)
    }

    @Test
    fun `getMeetingStatsEventAttributes should return 0 if meetingReconnectedTimeMs is eailer than meetingStartReconnectingTimeMs`() {
        testMeetingStatsCollector.updateMeetingReconnectedTimeMs()
        testMeetingStatsCollector.updateMeetingStartReconnectingTimeMs()

        val meetingReconnectDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingReconnectDurationMs]
        assertEquals(0L, meetingReconnectDurationMs)
    }

    @Test
    fun `getMeetingStatsEventAttributes should return the duration if meetingStartReconnectingTimeMs and meetingReconnectedTimeMs are updated properly`() {
        testMeetingStatsCollector.updateMeetingStartReconnectingTimeMs()
        Thread.sleep(1000)
        testMeetingStatsCollector.updateMeetingReconnectedTimeMs()

        val meetingReconnectDurationMs = testMeetingStatsCollector.getMeetingStatsEventAttributes()[EventAttributeName.meetingReconnectDurationMs] as Long
        assert(meetingReconnectDurationMs in 950L..1050L)
    }
}
