/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.internal.utils.DeviceUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Calendar

class DefaultEventAnalyticsController(
    private val logger: Logger,
    private val meetingSessionConfiguration: MeetingSessionConfiguration,
    private val meetingStatsCollector: MeetingStatsCollector
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

        ObserverUtils.notifyObserverOnMainThread(eventAnalyticsObservers) {
            it.onEventReceived(name, eventAttributes)
        }
    }

    override fun pushHistory(historyEventName: MeetingHistoryEventName) {
        meetingStatsCollector.addMeetingHistoryEvent(historyEventName, Calendar.getInstance().timeInMillis)
    }

    override fun getMeetingHistory(): List<MeetingHistoryEvent> {
        return meetingStatsCollector.getMeetingHistory()
    }

    override fun getCommonEventAttributes(): EventAttributes {
        val attributes = mutableMapOf(
            EventAttributeName.deviceName to DeviceUtils.deviceName,
            EventAttributeName.deviceManufacturer to DeviceUtils.deviceManufacturer,
            EventAttributeName.deviceModel to DeviceUtils.deviceModel,
            EventAttributeName.mediaSdkVersion to DeviceUtils.mediaSDKVersion,
            EventAttributeName.osName to DeviceUtils.osName,
            EventAttributeName.osVersion to DeviceUtils.osVersion,
            EventAttributeName.sdkName to DeviceUtils.sdkName,
            EventAttributeName.sdkVersion to DeviceUtils.sdkVersion,
            EventAttributeName.meetingId to meetingSessionConfiguration.meetingId,

            EventAttributeName.attendeeId to
                    meetingSessionConfiguration.credentials.attendeeId,
            EventAttributeName.externalUserId to
                    meetingSessionConfiguration.credentials.externalUserId
        )

        meetingSessionConfiguration.externalMeetingId?.let {
            attributes[EventAttributeName.externalMeetingId] = it
        }

        return attributes as EventAttributes
    }

    override fun addEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        this.eventAnalyticsObservers.add(observer)
    }

    override fun removeEventAnalyticsObserver(observer: EventAnalyticsObserver) {
        this.eventAnalyticsObservers.remove(observer)
    }
}
