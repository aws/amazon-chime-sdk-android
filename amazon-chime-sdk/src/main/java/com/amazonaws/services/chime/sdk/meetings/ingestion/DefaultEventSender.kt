/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.google.gson.Gson
import java.net.URL

class DefaultEventSender(
    private val ingestionConfiguration: IngestionConfiguration,
    private val logger: Logger
) : EventSender {
    private val TAG = "DefaultEventSender"
    private val gson = Gson()
    private val AUTHORIZATION_HEADER = "Authorization"
    private val BEARER = "Bearer"
    private val eventUrl: URL = URL(ingestionConfiguration.ingestionUrl)

    // 408: Request Timeout
    // 429: Too many request
    // 500: Internal Server Error
    // 502: Bad Gateway
    // 503: Service Unavailable
    // 504: Gateway timeout
    private val retryableCodeSet = setOf<Int>(
        408, 429, 500, 502, 503, 504
    )

    override suspend fun sendRecord(record: IngestionRecord): Boolean {
        return try {
            val body = gson.toJson(record)
            val response = HttpUtils.post(
                eventUrl,
                body,
                DefaultBackOffRetry(
                    ingestionConfiguration.retryCountLimit,
                    retryableStatusCodes = retryableCodeSet
                ),
                logger,
                mapOf(
                    AUTHORIZATION_HEADER to "$BEARER ${ingestionConfiguration.clientConfiguration.eventClientJoinToken}"
                )
            )
            response.httpException == null
        } catch (err: Exception) {
            logger.error(
                TAG,
                "Unable to send record ${err.localizedMessage}"
            )
            false
        }
    }
}
