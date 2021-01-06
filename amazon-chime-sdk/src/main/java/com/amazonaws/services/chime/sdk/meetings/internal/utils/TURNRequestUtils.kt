/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNCredentials
import com.amazonaws.services.chime.sdk.meetings.internal.video.TURNRequestParams
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object TURNRequestUtils {
    private val TOKEN_HEADER = "X-Chime-Auth-Token"
    private val SYSPROP_USER_AGENT = "http.agent"
    private val USER_AGENT_HEADER = "User-Agent"
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val CONTENT_TYPE = "application/json"
    private val MEETING_ID_KEY = "meetingId"
    private val TOKEN_KEY = "_aws_wt_session"

    private val TAG = "TURNRequestUtils"

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun doTurnRequest(turnRequestParams: TURNRequestParams, logger: Logger): TURNCredentials? {
        return withContext(ioDispatcher) {
            try {
                val response = StringBuffer()
                logger.info(TAG, "Making TURN Request")
                with(URL(turnRequestParams.turnControlUrl).openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    addRequestProperty(TOKEN_HEADER, "$TOKEN_KEY=${DefaultModality(turnRequestParams.joinToken).base()}")
                    setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                    val user_agent = System.getProperty(SYSPROP_USER_AGENT)
                    logger.info(TAG, "User Agent while doing TURN request is $user_agent")
                    setRequestProperty(USER_AGENT_HEADER, user_agent)
                    val out = BufferedWriter(OutputStreamWriter(outputStream))
                    out.write(
                        JSONObject().put(
                            MEETING_ID_KEY,
                            turnRequestParams.meetingId
                        ).toString()
                    )
                    out.flush()
                    out.close()
                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }
                    if (responseCode == 200) {
                        logger.info(TAG, "TURN Request Success")
                        var responseObject = JSONObject(response.toString())
                        val jsonArray =
                            responseObject.getJSONArray(TURNCredentials.TURN_CREDENTIALS_RESULT_URIS)
                        val uris = arrayOfNulls<String>(jsonArray.length())
                        for (i in 0 until jsonArray.length()) {
                            uris[i] = jsonArray.getString(i)
                        }
                        TURNCredentials(
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_USERNAME),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_PASSWORD),
                            responseObject.getString(TURNCredentials.TURN_CREDENTIALS_RESULT_TTL),
                            uris
                        )
                    } else {
                        logger.error(
                            TAG,
                            "TURN Request got error with Response code: $responseCode"
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "Exception while doing TURN Request: $exception")
                null
            }
        }
    }
}
