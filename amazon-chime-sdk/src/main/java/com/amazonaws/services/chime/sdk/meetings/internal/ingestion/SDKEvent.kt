/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName

class SDKEvent {
    val name: String
    val eventAttributes: EventAttributes

    constructor(eventName: EventName, eventAttributes: EventAttributes) {
        this.name = eventName.name
        this.eventAttributes = eventAttributes
    }
    constructor(eventName: MeetingHistoryEventName, eventAttributes: EventAttributes) {
        this.name = eventName.name
        this.eventAttributes = eventAttributes
    }
}
