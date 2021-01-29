/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

interface MeetingStatsCollector {
    /**
     * Increment meeting session retry count.
     */
    fun incrementRetryCount()

    /**
     * Increment poor connection count during the session.
     */
    fun incrementPoorConnectionCount()

    /**
     * Update max video tile count during the meeting.
     *
     * @param videoTileCount: Int - Current video tile count.
     */
    fun updateMaxVideoTile(videoTileCount: Int)

    /**
     * Update meeting start time.
     */
    fun updateMeetingStartTimeMs()

    /**
     * Clear meeting stats.
     */
    fun resetMeetingStats()

    /**
     * Get the meeting stats attributes.
     *
     * @return [EventAttributes] - Event attributes of meeting stats.
     */
    fun getMeetingStatsEventAttributes(): EventAttributes

    /**
     * Get a list of meeting history events.
     *
     * @return [List<MeetingHistoryEvent>] - The list of meeting history events.
     */
    fun getMeetingHistory(): List<MeetingHistoryEvent>

    /**
     * Add a history meeting event.
     *
     * @param historyEventName: MeetingHistoryEventName - History event name to add.
     * @param timestampMs: Long - Timestamp of the event in millisecond.
     */
    fun addMeetingHistoryEvent(historyEventName: MeetingHistoryEventName, timestampMs: Long)
}
