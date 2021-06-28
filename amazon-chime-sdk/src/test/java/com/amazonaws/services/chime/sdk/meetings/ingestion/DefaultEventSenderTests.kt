/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpException
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpResponse
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.Exception
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultEventSenderTests {
    @MockK
    private lateinit var ingestionConfiguration: IngestionConfiguration
    @MockK
    private lateinit var logger: Logger

    private lateinit var eventSender: EventSender

    @MockK
    private lateinit var ingestionRecord: IngestionRecord

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockkObject(HttpUtils)
        every { ingestionConfiguration.ingestionUrl } returns "https://example.com"
        every { ingestionConfiguration.retryCountLimit } returns 5
        eventSender = DefaultEventSender(ingestionConfiguration, logger)
    }

    @After
    fun cleanup() {
        unmockkObject(HttpUtils)
    }

    @Test
    fun `send should invoke HttpUtils send and return true if succeeded`() {
        coEvery { HttpUtils.post(any(), any(), any(), any(), any()) }.returns(HttpResponse("", null))

        runBlockingTest {
            val result = eventSender.sendRecord(ingestionRecord)

            coVerify { HttpUtils.post(any(), any(), any(), any(), any()) }
            Assert.assertEquals(true, result)
        }
    }

    @Test
    fun `send should return false if failed`() {
        coEvery { HttpUtils.post(any(), any(), any(), any(), any()) }.returns(
            HttpResponse(
                null,
                HttpException(errorCode = 420)
            )
        )

        runBlockingTest {
            val result = eventSender.sendRecord(ingestionRecord)

            coVerify { HttpUtils.post(any(), any(), any(), any(), any()) }
            Assert.assertEquals(false, result)
        }
    }

    @Test
    fun `send should return false if thrown exception`() {
        coEvery { HttpUtils.post(any(), any(), any(), any(), any()) }.throws(
            Exception()
        )

        runBlockingTest {
            val result = eventSender.sendRecord(ingestionRecord)

            coVerify { HttpUtils.post(any(), any(), any(), any(), any()) }
            Assert.assertEquals(false, result)
        }
    }
}
