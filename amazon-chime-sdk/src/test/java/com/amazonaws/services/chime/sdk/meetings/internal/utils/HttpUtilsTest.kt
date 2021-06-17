/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class HttpUtilsTest {
    val body = "hello"

    @MockK
    private lateinit var connectionMock: HttpURLConnection

    @MockK
    private lateinit var outputStreamMock: OutputStream

    @MockK
    private lateinit var inputStreamMock: InputStream

    @MockK
    private lateinit var urlMock: URL

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { inputStreamMock.read(any()) } returns -1
        every { inputStreamMock.read(any(), any(), any()) } returns -1
        every { connectionMock.requestMethod = any() } returns Unit
        every { connectionMock.doInput = any() } returns Unit
        every { connectionMock.doOutput = any() } returns Unit
        every { connectionMock.setRequestProperty(any(), any()) } returns Unit
        every { connectionMock.outputStream } returns outputStreamMock
        every { connectionMock.inputStream } returns inputStreamMock
        every { connectionMock.errorStream } returns inputStreamMock
        every { urlMock.openConnection() } returns connectionMock
    }

    @Test
    fun `HttpUtils should return responseCode 400`() {
        every { (connectionMock).responseCode } returns 400
        runBlocking {
            val result = HttpUtils.post(urlMock, body)

            Assert.assertEquals(400, result.httpException?.errorCode)
        }
    }

    @Test
    fun `HttpUtils should return responseCode 200`() {
        every { (connectionMock).responseCode } returns 200

        runBlocking {
            val result = HttpUtils.post(urlMock, body)

            Assert.assertNotNull(result.data)
            Assert.assertNull(result.httpException)
        }
    }

    @Test
    fun `HttpUtils should return httpException when exception is thrown`() {
        every { (connectionMock).responseCode } returns 200
        every { connectionMock.outputStream } returns null

        runBlocking {
            val result = HttpUtils.post(urlMock, body)

            Assert.assertNull(result.data)
            Assert.assertNotNull(result.httpException)
            Assert.assertNotNull(result.httpException?.errorReason)
        }
    }

    @Test
    fun `HttpUtils should retry if it is in the retryableSet`() {
        every { (connectionMock).responseCode } returns 400
        val retry = 5

        runBlocking {
            HttpUtils.post(urlMock, body, DefaultBackOffRetry(retry, 0, setOf(400)))

            verify(exactly = retry) { urlMock.openConnection() }
        }
    }

    @Test
    fun `HttpUtils should not retry if it is not in the retryableSet`() {
        every { (connectionMock).responseCode } returns 400
        val retry = 5

        runBlocking {
            HttpUtils.post(urlMock, body, DefaultBackOffRetry(retry, 0, setOf(404)))

            verify(exactly = 1) { urlMock.openConnection() }
        }
    }
}
