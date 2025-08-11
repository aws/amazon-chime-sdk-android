/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Calendar

class DefaultMeetingStatsCollector(
    private val logger: Logger
) : MeetingStatsCollector {
    // The time that the meeting is requested to be started
    private var meetingStartConnectingTimeMs: Long = 0L
    // The time meeting has started
    private var meetingStartTimeMs: Long = 0L
    // The time that meeting starts reconnecting
    private var meetingStartReconnectingTimeMs: Long = 0L
    // The time that meeting starts reconnecting
    private var meetingReconnectedTimeMs: Long = 0L
    // The number of attempts to reconnect to the meeting
    private var retryCount: Int = 0
    // The number of poor connection occurrences
    private var poorConnectionCount: Int = 0
    // The max count of video tile during the meeting
    private var maxVideoTileCount: Int = 0

    private val historyEvents = mutableListOf<MeetingHistoryEvent>()

    override fun incrementRetryCount() {
        retryCount++
    }

    override fun incrementPoorConnectionCount() {
        poorConnectionCount++
    }

    override fun updateMaxVideoTile(videoTileCount: Int) {
        maxVideoTileCount = videoTileCount.coerceAtLeast(maxVideoTileCount)
    }

    override fun updateMeetingStartConnectingTimeMs() {
        meetingStartConnectingTimeMs = Calendar.getInstance().timeInMillis
    }

    override fun updateMeetingStartTimeMs() {
        meetingStartTimeMs = Calendar.getInstance().timeInMillis
    }

    override fun updateMeetingStartReconnectingTimeMs() {
        meetingStartReconnectingTimeMs = Calendar.getInstance().timeInMillis
        meetingReconnectedTimeMs = 0L
    }

    override fun updateMeetingReconnectedTimeMs() {
        meetingReconnectedTimeMs = Calendar.getInstance().timeInMillis
    }

    override fun resetMeetingStats() {
        meetingStartConnectingTimeMs = 0L
        meetingStartTimeMs = 0L
        retryCount = 0
        poorConnectionCount = 0
        maxVideoTileCount = 0
        meetingStartReconnectingTimeMs = 0L
        meetingReconnectedTimeMs = 0L
    }

    override fun getMeetingStatsEventAttributes(): EventAttributes {
        val meetingReconnectDurationMs =
            if (meetingStartReconnectingTimeMs == 0L || meetingReconnectedTimeMs == 0L || meetingReconnectedTimeMs < meetingStartReconnectingTimeMs) 0L
            else meetingReconnectedTimeMs - meetingStartReconnectingTimeMs

        return mutableMapOf(
            EventAttributeName.maxVideoTileCount to maxVideoTileCount,
            EventAttributeName.retryCount to retryCount,
            EventAttributeName.poorConnectionCount to poorConnectionCount,
            EventAttributeName.meetingDurationMs to if (meetingStartTimeMs == 0L) 0L else Calendar.getInstance().timeInMillis - meetingStartTimeMs,
            EventAttributeName.meetingStartDurationMs to if (meetingStartTimeMs == 0L) 0L else meetingStartTimeMs - meetingStartConnectingTimeMs,
            EventAttributeName.meetingReconnectDurationMs to meetingReconnectDurationMs
        )
    }

    override fun addMeetingHistoryEvent(historyEventName: MeetingHistoryEventName, timestampMs: Long) {
        historyEvents.add(MeetingHistoryEvent(historyEventName, timestampMs))
    }

    override fun getMeetingHistory(): List<MeetingHistoryEvent> = historyEvents
}
