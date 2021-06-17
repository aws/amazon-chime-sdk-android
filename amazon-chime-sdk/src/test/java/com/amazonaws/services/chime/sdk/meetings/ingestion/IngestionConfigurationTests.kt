/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class IngestionConfigurationTests {
    @MockK
    private lateinit var clientConfiguration: MeetingEventClientConfiguration
    private val ingestionUrl = "https://ingesitonurl.com"
    private val disabled = false
    private val flushSize = 70
    private val flushIntervalMs = 3000L
    private val retryCountLimit = 3

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `constructor should return values passed in`() {
        val ingestionConfiguration = IngestionConfiguration(
            clientConfiguration,
            ingestionUrl,
            disabled,
            flushSize,
            flushIntervalMs,
            retryCountLimit
        )

        Assert.assertEquals(clientConfiguration, ingestionConfiguration.clientConfiguration)
        Assert.assertEquals(ingestionUrl, ingestionConfiguration.ingestionUrl)
        Assert.assertEquals(disabled, ingestionConfiguration.disabled)
        Assert.assertEquals(flushSize, ingestionConfiguration.flushSize)
        Assert.assertEquals(flushIntervalMs, ingestionConfiguration.flushIntervalMs)
        Assert.assertEquals(retryCountLimit, ingestionConfiguration.retryCountLimit)
    }

    @Test
    fun `constructor should restrict numerical values`() {
        val ingestionConfiguration = IngestionConfiguration(
            clientConfiguration,
            ingestionUrl,
            disabled,
            100000000,
            -5,
            100000000
        )

        Assert.assertEquals(FLUSH_SIZE_LIMIT_MAXIMUM, ingestionConfiguration.flushSize)
        Assert.assertEquals(FLUSH_INTERVAL_LIMIT_MINIMUM, ingestionConfiguration.flushIntervalMs)
        Assert.assertEquals(RETRY_COUNT_LIMIT_MAXIMUM, ingestionConfiguration.retryCountLimit)
    }
}
