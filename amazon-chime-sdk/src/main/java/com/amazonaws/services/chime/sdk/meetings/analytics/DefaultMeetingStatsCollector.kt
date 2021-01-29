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
    // The time meeting has started
    private var meetingStartTimeMs: Long = 0L
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

    override fun updateMeetingStartTimeMs() {
        meetingStartTimeMs = Calendar.getInstance().timeInMillis
    }

    override fun resetMeetingStats() {
        meetingStartTimeMs = 0L
        retryCount = 0
        poorConnectionCount = 0
        maxVideoTileCount = 0
    }

    override fun getMeetingStatsEventAttributes(): EventAttributes {
        return mutableMapOf(
            EventAttributeName.maxVideoTileCount to maxVideoTileCount,
            EventAttributeName.retryCount to retryCount,
            EventAttributeName.poorConnectionCount to poorConnectionCount,
            EventAttributeName.meetingDurationMs to if (meetingStartTimeMs == 0L) 0L else Calendar.getInstance().timeInMillis - meetingStartTimeMs
        )
    }

    override fun addMeetingHistoryEvent(historyEventName: MeetingHistoryEventName, timestampMs: Long) {
        historyEvents.add(MeetingHistoryEvent(historyEventName, timestampMs))
    }

    override fun getMeetingHistory(): List<MeetingHistoryEvent> = historyEvents
}
