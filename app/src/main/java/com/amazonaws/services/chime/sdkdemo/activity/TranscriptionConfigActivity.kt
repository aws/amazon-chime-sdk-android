/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.TranscribeEngine
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguage
import com.amazonaws.services.chime.sdkdemo.data.TranscribeRegion
import com.amazonaws.services.chime.sdkdemo.fragment.TranscriptionConfigFragment
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import com.amazonaws.services.chime.sdkdemo.utils.showToast
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranscriptionConfigActivity : AppCompatActivity(),
    TranscriptionConfigFragment.TranscriptionConfigurationEventListener {

    private val logger = ConsoleLogger(LogLevel.INFO)

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var meetingId: String

    private val TAG = "TranscriptionConfigActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transcription_config)
        meetingId = intent.getStringExtra(HomeActivity.MEETING_ID_KEY) as String

        if (savedInstanceState == null) {
            val transcriptionConfigFragment = TranscriptionConfigFragment.newInstance(meetingId)
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
            val response: String? =
                enableMeetingTranscription(
                    meetingId,
                    engine.engine,
                    language.code,
                    region.code)

            if (response == null) {
                showToast(getString(R.string.user_notification_transcription_start_error))
            } else {
                showToast(getString(R.string.user_notification_transcription_start_success))
            }
            onBackPressed()
        }
    }

    private suspend fun enableMeetingTranscription(
        meetingId: String?,
        transcribeEngine: String?,
        transcriptionLanguage: String?,
        transcriptionRegion: String?
    ): String? {
        val meetingUrl = if (getString(R.string.test_url).endsWith("/")) getString(R.string.test_url) else "${getString(R.string.test_url)}/"
        val url = "${meetingUrl}start_transcription?title=${encodeURLParam(meetingId)}" +
                "&language=${encodeURLParam(transcriptionLanguage)}" +
                "&region=${encodeURLParam(transcriptionRegion)}" +
                "&engine=${encodeURLParam(transcribeEngine)}"
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)

        return if (response.httpException == null) {
            response.data
        } else {
            logger.error(TAG, "Error sending start transcription request ${response.httpException}")
            null
        }
    }
}
