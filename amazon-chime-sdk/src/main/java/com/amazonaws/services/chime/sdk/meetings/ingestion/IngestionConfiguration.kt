/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

const val FLUSH_SIZE_LIMIT_MAXIMUM = 100
const val FLUSH_INTERVAL_LIMIT_MINIMUM: Long = 100
const val RETRY_COUNT_LIMIT_MAXIMUM = 5
/**
 * [IngestionConfiguration] defines the configuration that can customize [DefaultEventReporter].
 *
 * @property clientConfiguration: [EventClientConfiguration] - configuration needed for metadata.
 * @property ingestionUrl: [String] - ingestion server url.
 * @property disabled: [Boolean] - whether ingestion is enabled or disabled. defaults [true].
 * @property flushSize: [Int] - number of payloads to send as a batch. <= 100 and > 0. defaults 20.
 * @property flushIntervalMs: [Long] - duration to wait, pull, and send the data. >= 100. defaults 5000.
 * @property retryCountLimit: [Int] - retry count limit. 0 >= and <= 5. defaults 2.
 */
data class IngestionConfiguration @JvmOverloads constructor(
    val clientConfiguration: EventClientConfiguration,
    var ingestionUrl: String,
    val disabled: Boolean = true,
    var flushSize: Int = 20,
    var flushIntervalMs: Long = 5000,
    var retryCountLimit: Int = 2
) {

    init {
        // Force the max size to 100 as maximum
        // Max payload is 256 kb.
        // Assumption is that each event is max 2kb so we could send ~100 in batch
        // Metadata is about 300~400 bytes so worst case 200 + 300 + extra = 1kb
        // Assumes the worst case 1kb * 2 = 2 kb
        flushSize = flushSize.coerceAtLeast(1).coerceAtMost(FLUSH_SIZE_LIMIT_MAXIMUM)
        // Force the interval to 100 ms minimum
        flushIntervalMs = flushIntervalMs.coerceAtLeast(FLUSH_INTERVAL_LIMIT_MINIMUM)
        // Force retry count to at most 5
        retryCountLimit = retryCountLimit.coerceAtLeast(0).coerceAtMost(RETRY_COUNT_LIMIT_MAXIMUM)
    }
}
