/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostLogger(
    private val name: String,
    private val configuration: MeetingSessionConfiguration,
    private val url: String,
    private var level: LogLevel = LogLevel.INFO
) : Logger {
    private val gson = Gson()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var sequenceNumber = AtomicInteger(0)
    private val logEntries: MutableList<LogEntry> = mutableListOf()

    override fun verbose(tag: String, msg: String) {
        log(tag, msg, LogLevel.VERBOSE)
    }

    override fun debug(tag: String, msg: String) {
        log(tag, msg, LogLevel.DEBUG)
    }

    override fun info(tag: String, msg: String) {
        log(tag, msg, LogLevel.INFO)
    }

    override fun warn(tag: String, msg: String) {
        log(tag, msg, LogLevel.WARN)
    }

    override fun error(tag: String, msg: String) {
        log(tag, msg, LogLevel.ERROR)
    }

    override fun setLogLevel(level: LogLevel) {
        this.level = level
    }

    override fun getLogLevel(): LogLevel {
        return level
    }

    private fun saveLog(msg: String, type: LogLevel) {
        logEntries.add(LogEntry(sequenceNumber.get(), msg, Calendar.getInstance().timeInMillis, type))
        sequenceNumber.incrementAndGet()
    }

    fun publishLog(tag: String) {
        val body = makeRequestBody(logEntries)
        uiScope.launch {
            makeRequest(body, tag)
        }
    }

    private fun log(tag: String, msg: String, type: LogLevel) {
        if (type.priority < this.level.priority) return
        saveLog(msg, type)
    }

    private suspend fun makeRequest(body: String, tag: String) {
        withContext(Dispatchers.IO) {
            try {
                val serverUrl = URL(url)
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    setRequestProperty("Accept", "application/json")

                    outputStream.use {
                        val input = body.toByteArray()
                        it.write(input, 0, input.size)
                    }

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 200) {
                        Log.i(tag, "Publishing log was successful")
                        response.toString()
                        logEntries.clear()
                    } else {
                        Log.e(tag, "Unable to publish log. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                Log.e(tag, "There was an exception while posting logs: $exception")
                null
            }
        }
    }

    private fun makeRequestBody(batch: MutableList<LogEntry>): String {
        val body = mutableMapOf(
            "meetingId" to configuration.meetingId,
            "attendeeId" to configuration.credentials.attendeeId,
            "appName" to name,
            "logs" to batch
        )
        return gson.toJson(body)
    }
}
