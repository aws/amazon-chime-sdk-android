/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime.datamessage

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class DataMessageTest {
    @Test
    fun `text should return data as a string`() {
        val dataString = "hello"
        val dataBytes = dataString.toByteArray()
        val dataMessage = DataMessage(
            10000,
            "topic",
            dataBytes,
            "attendeeId",
            "externalId",
            false)

        assertEquals(dataString, dataMessage.text())
    }

    @Test
    fun `fromJson should return data as a given type`() {
        val obj = CustomerDataClass("hello", 1)
        val dataBytes = Gson().toJson(obj).toByteArray()
        val dataMessage = DataMessage(
            10000,
            "topic",
            dataBytes,
            "attendeeId",
            "externalId",
            false
        )

        val objFromJson = dataMessage.fromJson(CustomerDataClass::class.java)

        assertEquals(obj, objFromJson)
    }

    data class CustomerDataClass(val string: String, val number: Int)
}
