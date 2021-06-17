/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object HttpUtils {
    private val TAG = "HttpUtils"
    private val SYSPROP_USER_AGENT = "http.agent"
    private val USER_AGENT_HEADER = "User-Agent"

    /**
     * Post request. In order to get value, one has to do
     * dispatcher.launch {
     *      result = post(...)
     *      use result to do sth
     * }
     *
     * @param url: [URL] - URL of the server
     * @param body: [String] - json String body
     * @param backOffRetry: [BackOffRetry] - Retry policy
     * @param logger - [Logger] - logger to log the data
     * @param headers - [Map<String, String>] - headers to add the http call
     * @return HttpResponse containing data and error
     */
    suspend fun post(
        url: URL,
        body: String,
        backOffRetry: BackOffRetry = DefaultBackOffRetry(),
        logger: Logger? = null,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        var response: HttpResponse
        do {
            response = makePostRequest(url, body, logger, headers)
            if (response.httpException == null || !backOffRetry.isRetryableCode(
                    response.httpException?.errorCode ?: 0
                )
            ) {
                return response
            }
            backOffRetry.incrementRetryCount()
            val waitTime = backOffRetry.calculateBackOff()
            if (waitTime > 0) {
                delay(waitTime)
            }
        } while (backOffRetry.isRetryCountLimitReached())
        return response
    }

    private suspend fun makePostRequest(url: URL, body: String, logger: Logger?, headers: Map<String, String>): HttpResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = StringBuffer()
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    for (header in headers) {
                        setRequestProperty(header.key, header.value)
                    }
                    setRequestProperty("Content-Type", "application/json")
                    val userAgent = System.getProperty(SYSPROP_USER_AGENT)
                    setRequestProperty(USER_AGENT_HEADER, userAgent)
                    outputStream.use {
                        val input = body.toByteArray()
                        it.write(input, 0, input.size)
                    }

                    val inStream = if (responseCode in 200..299) inputStream else errorStream
                    BufferedReader(InputStreamReader(inStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode in 200..299) {
                        return@withContext HttpResponse(response.toString(), null)
                    }

                    return@withContext HttpResponse(
                        response.toString(),
                        HttpException(errorCode = responseCode)
                    )
                }
            } catch (exception: Exception) {
                logger?.error(TAG, "Unable to send request $exception")
                return@withContext HttpResponse(null, HttpException(null, exception.toString()))
            }
        }
    }
}
