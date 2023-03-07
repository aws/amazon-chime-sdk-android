/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName
import com.amazonaws.services.chime.sdk.meetings.analytics.toStringKeyMap

class SDKEvent(
    val name: String,
    val eventAttributes: Map<String, Any>
) {
    constructor(eventName: EventName, eventAttributes: EventAttributes) : this(eventName.name, eventAttributes.toStringKeyMap())
    constructor(eventName: MeetingHistoryEventName, eventAttributes: EventAttributes) : this(eventName.name, eventAttributes.toStringKeyMap())

    fun putAttributes(attributes: Map<String, Any>): SDKEvent {
        val updatedEventAttributes = eventAttributes.toMutableMap()
        updatedEventAttributes.putAll(attributes)
        return SDKEvent(name, updatedEventAttributes)
    }
}
