/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [EventAnalyticsFacade] allows builders to listen to meeting analytics events
 * through adding/removing [EventAnalyticsObserver].
 *
 * For instance, if meeting start succeeded, you'll receive data
 * [EventAnalyticsObserver.onEventReceived] with name as [EventName] and attributes as [EventAttributes],
 * which is just [Map]. This will include attributes specific to the event.
 *
 * For more information about the attributes, please refer to [EventAttributeName].
 *
 * It can have additional attributes based on the event.
 */
interface EventAnalyticsFacade {
    /**
     * Subscribe to meeting event related data with an observer.
     *
     * @param observer: [EventAnalyticsObserver] - An observer to add to start receiving meeting events
     */
    fun addEventAnalyticsObserver(observer: EventAnalyticsObserver)

    /**
     * Unsubscribe from meeting event by removing the specified observer.
     *
     * @param observer: [EventAnalyticsObserver] - An observer to remove to stop receiving meeting events
     */
    fun removeEventAnalyticsObserver(observer: EventAnalyticsObserver)

    /**
     * Retrieve meeting history.
     */
    fun getMeetingHistory(): List<MeetingHistoryEvent>

    /**
     * Retrieve common attributes, including deviceName, osName, and more.
     */
    fun getCommonEventAttributes(): EventAttributes
}
