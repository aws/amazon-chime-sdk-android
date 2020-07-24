/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.data

import java.text.SimpleDateFormat
import java.util.Date

data class Message(
    val senderName: String,
    val timestamp: Long,
    val text: String,
    val isLocal: Boolean
) {
    val displayTime: String
        get() {
            return SimpleDateFormat("HH:mm").format(Date(timestamp))
        }
}
