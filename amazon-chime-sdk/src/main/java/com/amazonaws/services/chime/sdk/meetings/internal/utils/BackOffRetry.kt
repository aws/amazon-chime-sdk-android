/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

interface BackOffRetry {
    fun calculateBackOff(): Long
    fun getRetryCount(): Int
    fun isRetryCountLimitReached(): Boolean
    fun incrementRetryCount()
    fun isRetryableCode(responseCode: Int): Boolean
}
