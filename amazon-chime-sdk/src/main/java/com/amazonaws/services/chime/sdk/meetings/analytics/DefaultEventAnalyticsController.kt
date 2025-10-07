/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.ingestion.AppState
import com.amazonaws.services.chime.sdk.meetings.ingestion.AppStateHandler
import com.amazonaws.services.chime.sdk.meetings.ingestion.AppStateMonitor
import com.amazonaws.services.chime.sdk.meetings.ingestion.EventReporter
import com.amazonaws.services.chime.sdk.meetings.ingestion.NetworkConnectionType
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.utils.EventAttributesUtils
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.util.Calendar

class DefaultEventAnalyticsController(
    private val logger: Logger,
    private val meetingSessionConfiguration: MeetingSessionConfiguration,
    private val meetingStatsCollector: MeetingStatsCollector,
    private val appStateMonitor: AppStateMonitor,
    private val eventReporter: EventReporter? = null
) : EventAnalyticsController, AppStateHandler {
    private var eventAnalyticsObservers = ConcurrentSet.createConcurrentSet<EventAnalyticsObserver>()

    override fun publishEvent(name: EventName, attributes: EventAttributes?, notifyObservers: Boolean) {
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
            EventName.meetingEnded, EventName.meetingFailed, EventName.meetingReconnected ->
            {
                val meetingStats = meetingStatsCollector.getMeetingStatsEventAttributes()
                if (name != EventName.meetingReconnected) {
                    meetingStats.remove(EventAttributeName.meetingReconnectDurationMs)
                }
                eventAttributes.putAll(meetingStats)
            }
            else -> Unit
        }

        eventAttributes[EventAttributeName.appState] = appStateMonitor.appState.description
        appStateMonitor.getBatteryLevel()?.let {
            eventAttributes[EventAttributeName.batteryLevel] = it
        }
        eventAttributes[EventAttributeName.batteryState] = appStateMonitor.getBatteryState().description
        eventAttributes[EventAttributeName.lowPowerModeEnabled] = appStateMonitor.isBatterySaverOn().toString()

        eventReporter?.report(SDKEvent(name, eventAttributes))

        if (notifyObservers) {
            ObserverUtils.notifyObserverOnMainThread(eventAnalyticsObservers) {
                it.onEventReceived(name, eventAttributes)
            }
        }
    }

    override fun pushHistory(historyEventName: MeetingHistoryEventName) {
        val currentTimeMs = Calendar.getInstance().timeInMillis
        val eventAttributes = mutableMapOf(
            EventAttributeName.timestampMs to currentTimeMs,
            EventAttributeName.appState to appStateMonitor.appState.description,
            EventAttributeName.batteryState to appStateMonitor.getBatteryState().description,
            EventAttributeName.lowPowerModeEnabled to appStateMonitor.isBatterySaverOn().toString()
        ) as EventAttributes

        appStateMonitor.getBatteryLevel()?.let {
            eventAttributes[EventAttributeName.batteryLevel] = it
        }

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

    // AppStateHandler implementation
    override fun onAppStateChanged(newAppState: AppState) {
        publishEvent(
            name = EventName.appStateChanged,
            attributes = mutableMapOf(EventAttributeName.appState to newAppState),
            false
        )
    }

    override fun onMemoryWarning() {
        publishEvent(
            name = EventName.appMemoryLow,
            attributes = mutableMapOf(),
            false
        )
    }

    override fun onNetworkConnectionTypeChanged(type: NetworkConnectionType) {
        publishEvent(EventName.networkConnectionTypeChanged,
            mutableMapOf(EventAttributeName.networkConnectionType to type.description),
            false)
    }
}
