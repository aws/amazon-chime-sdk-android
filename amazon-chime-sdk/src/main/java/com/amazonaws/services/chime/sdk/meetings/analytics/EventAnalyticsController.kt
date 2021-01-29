/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [EventAnalyticsController] keeps track of events and notifies [EventAnalyticsObserver].
 * An event describes the success and failure conditions for the meeting session.
 */
interface EventAnalyticsController {
    /**
     * Publish an event with updated [EventAttributes].
     *
     * @param name: [EventName] - Name of event to publish
     * @param attributes: [EventAttributes] - Attributes of event to pass to builders.
     */
    fun publishEvent(name: EventName, attributes: EventAttributes? = null)

    /**
     * Push [MeetingHistoryEventName] to internal [MeetingStatsCollector].
     *
     * @param historyEventName: MeetingHistoryEventName - History event name to add.
     */
    fun pushHistory(historyEventName: MeetingHistoryEventName)

    /**
     * Retrieve meeting history.
     */
    fun getMeetingHistory(): List<MeetingHistoryEvent>

    /**
     * Retrieve common attributes, including deviceName, osName, and more.
     */
    fun getCommonEventAttributes(): EventAttributes

    /**
     * Add specified [EventAnalyticsObserver].
     *
     * @param observer: [EventAnalyticsObserver] - The observer to subscribe to events with.
     */
    fun addEventAnalyticsObserver(observer: EventAnalyticsObserver)

    /**
     * Remove specified [EventAnalyticsObserver].
     *
     * @param observer: [EventAnalyticsObserver] - The observer to unsubscribe from events with.
     */
    fun removeEventAnalyticsObserver(observer: EventAnalyticsObserver)
}
