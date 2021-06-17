/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import kotlin.math.pow

class DefaultBackOffRetry(
    private val maxRetry: Int = 0,
    private val backOff: Long = 0,
    private val retryableStatusCodes: Set<Int> = HashSet()
) : BackOffRetry {
    private val multiplier = 1
    private var currentRetry = 0

    override fun calculateBackOff(): Long {
        return (backOff * multiplier.toDouble().pow(currentRetry.toDouble())).toLong()
    }

    override fun getRetryCount(): Int {
        return currentRetry
    }

    override fun isRetryCountLimitReached(): Boolean {
        return maxRetry > currentRetry
    }

    override fun incrementRetryCount() {
        currentRetry++
    }

    override fun isRetryableCode(responseCode: Int): Boolean {
        return retryableStatusCodes.isEmpty() || retryableStatusCodes.contains(responseCode)
    }
}
