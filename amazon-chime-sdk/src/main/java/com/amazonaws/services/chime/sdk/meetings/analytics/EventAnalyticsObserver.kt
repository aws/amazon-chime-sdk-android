/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [EventAnalyticsObserver] handles events regarding to analytics.
 */
interface EventAnalyticsObserver {
    /**
     * Called when specific events occur during the meeting. Each event includes attributes of the event.
     *
     * For more information about the attributes, please refer to [EventAttributeName].
     *
     * One example could be [EventName] as [EventName.meetingStartSucceeded] and
     * attributes would be [MutableMap] of [EventAttributeName] and it's value.
     *
     * @param name: [EventName] - name of meeting event
     * @param attributes: [EventAttributes] - attributes of meeting event
     *
     * NOTE: all callbacks will be called on main thread.
     */
    fun onEventReceived(name: EventName, attributes: EventAttributes)
}
