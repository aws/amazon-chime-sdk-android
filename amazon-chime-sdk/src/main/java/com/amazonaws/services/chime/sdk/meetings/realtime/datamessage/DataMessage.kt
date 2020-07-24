/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime.datamessage

import com.google.gson.Gson

/**
 * Data message received from server.
 *
 * @property timestampMs: Long - Monotonically increasing server ingest time
 * @property topic: String - Topic this message was sent on
 * @property data: ByteArray - Data payload
 * @property senderAttendeeId - Sender attendee
 * @property senderExternalUserId - Sender attendee external user Id
 * @property throttled - true if server throttled or rejected message,
 * false if server has posted the message to its recipients or it's not a sender receipt
 */
data class DataMessage(
    val timestampMs: Long,
    val topic: String,
    val data: ByteArray,
    val senderAttendeeId: String,
    val senderExternalUserId: String,
    val throttled: Boolean
) {

    /**
     * Helper method to convert ByteArray data to String
     *
     * @return string data
     */
    fun text(): String {
        return String(data)
    }

    /**
     * Helper method to convert ByteArray data to object of given type
     *
     * @param clazz: Object type
     * @return deserialized object
     */
    fun <T> fromJson(clazz: Class<T>): T {
        return Gson().fromJson(text(), clazz)
    }
}
