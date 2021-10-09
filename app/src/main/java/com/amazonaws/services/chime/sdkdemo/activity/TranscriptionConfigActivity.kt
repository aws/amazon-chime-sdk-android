/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.TranscribeEngine
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguage
import com.amazonaws.services.chime.sdkdemo.data.TranscribeRegion
import com.amazonaws.services.chime.sdkdemo.fragment.TranscriptionConfigFragment
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranscriptionConfigActivity : AppCompatActivity(),
    TranscriptionConfigFragment.TranscriptionConfigurationEventListener {

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var meetingId: String
    private lateinit var meetingUrl: String

    private val TAG = "InTranscriptionConfig"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transcription_config)
        meetingId = intent.getStringExtra(HomeActivity.MEETING_ID_KEY) as String
        meetingUrl = intent.getStringExtra(HomeActivity.MEETING_URL_KEY) as String

        if (savedInstanceState == null) {
            val transcriptionConfigFragment = TranscriptionConfigFragment.newInstance(meetingId, meetingUrl)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, transcriptionConfigFragment, "transcriptionConfig")
                .commit()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onStartTranscription(engine: TranscribeEngine, language: TranscribeLanguage, region: TranscribeRegion) {
        uiScope.launch {
            val transcriptionResponseJson: String? =
                enableMeetingTranscription(meetingUrl,
                    meetingId,
                    engine.engine,
                    language.code,
                    region.code)

            if (transcriptionResponseJson == null) {
                showToast(applicationContext, getString(R.string.user_notification_transcription_start_error))
            } else {
                showToast(applicationContext, getString(R.string.user_notification_transcription_start_success))
            }
            onBackPressed()
        }
    }

    private suspend fun enableMeetingTranscription(
        meetingUrl: String,
        meetingId: String?,
        transcribeEngine: String?,
        transcriptionLanguage: String?,
        transcriptionRegion: String?
    ): String? {
        return withContext(ioDispatcher) {
            val url = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
            val serverUrl =
                URL(
                    "${url}start_transcription?title=${encodeURLParam(meetingId)}" +
                            "&language=${encodeURLParam(transcriptionLanguage)}" +
                            "&region=${encodeURLParam(transcriptionRegion)}" +
                            "&engine=${encodeURLParam(transcribeEngine)}"
                )

            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 200) {
                        response.toString()
                    } else {
                        logger.error(TAG, "Unable to start transcription. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "There was an exception while starting transcription for the meeting $meetingId: $exception")
                null
            }
        }
    }

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(
            context,
            msg,
            Toast.LENGTH_LONG
        ).show()
    }
}
