/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.ingestion

import com.google.gson.Gson

/**
 * EventTypeConverters facilitate the conversion on some common event types
 */
object EventTypeConverters {
    val gson = Gson()
    fun toMeetingEvent(data: String): SDKEvent = gson.fromJson(data, SDKEvent::class.java)
    fun fromMeetingEvent(event: SDKEvent): String = gson.toJson(event)
}
