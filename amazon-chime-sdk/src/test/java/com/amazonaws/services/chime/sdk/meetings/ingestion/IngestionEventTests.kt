/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Test

class IngestionEventTests {
    private val metadata = mapOf(
        EventAttributeName.meetingId to "eeeeeeieei"
    ) as EventAttributes
    private val payloads = listOf(mapOf(
        "1" to "2",
        "hello" to 5
    ))

    @Test
    fun `IngestionEvent version should be encoded as v`() {
        val ingestionEvent = IngestionEvent(EventClientType.Meet, metadata, payloads)
        val gson = Gson()

        val ingestionEventJson = gson.toJson(ingestionEvent)

        Assert.assertTrue(ingestionEventJson.contains("\"v\":"))
    }
}
