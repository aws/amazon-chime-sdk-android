/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel

/**
 * Log format for the server
 *
 * @property sequenceNumber sequence number of the log
 * @property message message to log
 * @property timestampMs time of when log occurred
 * @property logLevel level of log
 */
data class LogEntry(
    val sequenceNumber: Int,
    val message: String,
    val timestampMs: Long,
    val logLevel: LogLevel
)
