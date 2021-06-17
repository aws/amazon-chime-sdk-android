/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.ingestion.EventReporter
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.internal.utils.EventAttributesUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Calendar

class DefaultEventAnalyticsController(
    private val logger: Logger,
    private val meetingSessionConfiguration: MeetingSessionConfiguration,
    private val meetingStatsCollector: MeetingStatsCollector,
    private val eventReporter: EventReporter? = null
) : EventAnalyticsController {
    private var eventAnalyticsObservers: MutableSet<EventAnalyticsObserver> = mutableSetOf()

    override fun publishEvent(name: EventName, attributes: EventAttributes?) {
        val now = Calendar.getInstance().timeInMillis

        // Also pushes to the history
        meetingStatsCollector.addMeetingHistoryEvent(
            MeetingHistoryEventName.fromMeetingEvent(name),
            now
        )

        // Create event to publish
        val eventAttributes = attributes ?: mutableMapOf()
        eventAttributes[EventAttributeName.timestampMs] = now

        // Add meeting stats for meeting cycle related events
        when (name) {
            EventName.meetingStartSucceeded, EventName.meetingStartFailed,
            EventName.meetingEnded, EventName.meetingFailed ->
                eventAttributes.putAll(meetingStatsCollector.getMeetingStatsEventAttributes())
            else -> Unit
        }

        eventReporter?.report(SDKEvent(name, eventAttributes))

        ObserverUtils.notifyObserverOnMainThread(eventAnalyticsObservers) {
            it.onEventReceived(name, eventAttributes)
        }
    }

    override fun pushHistory(historyEventName: MeetingHistoryEventName) {
        val currentTimeMs = Calendar.getInstance().timeInMillis
        val eventAttributes = mutableMapOf(
            EventAttributeName.timestampMs to currentTimeMs
        ) as EventAttributes

        eventReporter?.report(SDKEvent(historyEventName, eventAttributes))

        meetingStatsCollector.addMeetingHistoryEvent(historyEventName, currentTimeMs)
    }

    override fun getMeetingHistory(): List<MeetingHistoryEvent> {
        return meetingStatsCollector.getMeetingHistory()
    }

    override fun getCommonEventAttributes(): EventAttributes {
        return EventAttributesUtils.getCommonAttributes(meetingSessionConfiguration)
    }

    override fun addEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        this.eventAnalyticsObservers.add(observer)
    }

    override fun removeEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        this.eventAnalyticsObservers.remove(observer)
    }
}
