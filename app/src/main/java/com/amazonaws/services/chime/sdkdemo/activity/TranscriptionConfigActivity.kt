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
import com.amazonaws.services.chime.sdkdemo.data.SpinnerItem
import com.amazonaws.services.chime.sdkdemo.data.TranscriptionStreamParams
import com.amazonaws.services.chime.sdkdemo.fragment.TranscriptionConfigFragment
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import com.amazonaws.services.chime.sdkdemo.utils.showToast
import com.google.gson.Gson
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranscriptionConfigActivity : AppCompatActivity(),
    TranscriptionConfigFragment.TranscriptionConfigurationEventListener {

    private val transcribeContentIdentificationType = "PII"
    private val transcribeMedicalContentIdentificationType = "PHI"
    private val transcribeMedicalEngine = "transcribe_medical"
    private val transcribePartialResultsStabilizationDefault = "default"
    private val transcribePartialResultsStabilizationHigh = "high"

    private val logger = ConsoleLogger(LogLevel.INFO)

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var meetingId: String

    private val TAG = "TranscriptionConfigActivity"

    private val gson = Gson()
    
    private lateinit var meetingEndpointUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transcription_config)
        meetingId = intent.getStringExtra(HomeActivity.MEETING_ID_KEY) as String
        meetingEndpointUrl = intent.getStringExtra(HomeActivity.MEETING_ENDPOINT_KEY) as String
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

    override fun onStartTranscription(
        engine: SpinnerItem,
        language: SpinnerItem,
        region: SpinnerItem,
        partialResultsStability: SpinnerItem,
        contentIdentificationType: SpinnerItem,
        contentRedactionType: SpinnerItem,
        languageModelName: String
    ) {
        uiScope.launch {
            val response: String? =
                enableMeetingTranscription(
                    meetingId,
                    engine.spinnerText,
                    language.spinnerText,
                    region.spinnerText,
                    partialResultsStability.spinnerText,
                    contentIdentificationType.spinnerText,
                    contentRedactionType.spinnerText,
                    languageModelName
                )

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
        transcriptionRegion: String?,
        transcribePartialResultsStabilization: String?,
        transcribeContentIdentification: String?,
        transcribeContentRedaction: String?,
        customLanguageModel: String
    ): String? {
        val partialResultsStabilizationEnabled = transcribePartialResultsStabilization != null
        val isTranscribeMedical = transcribeEngine.equals(transcribeMedicalEngine)
        val transcriptionStreamParams = TranscriptionStreamParams(
            contentIdentificationType = transcribeContentIdentification?.let {
                if (isTranscribeMedical) transcribeMedicalContentIdentificationType
                else transcribeContentIdentificationType
            },
            contentRedactionType = transcribeContentRedaction?.let { transcribeContentIdentificationType },
            enablePartialResultsStabilization = partialResultsStabilizationEnabled,
            partialResultsStability = transcribePartialResultsStabilization?.let {
                if (it == transcribePartialResultsStabilizationDefault) transcribePartialResultsStabilizationHigh
                else it
            },
            piiEntityTypes = transcribeContentIdentification.let { identification ->
                if (identification == "" || isTranscribeMedical) null
                else identification
            } ?: run { transcribeContentRedaction.let { redaction ->
                if (redaction == "") null
                else redaction
                }
            },
            languageModelName = customLanguageModel.let {
                it.ifEmpty { null }
            }
        )
        val transcriptionAdditionalParams = gson.toJson(transcriptionStreamParams)

        val meetingUrl = if (meetingEndpointUrl.endsWith("/")) meetingEndpointUrl else meetingEndpointUrl.plus("/")
        val url = "${meetingUrl}start_transcription?title=${encodeURLParam(meetingId)}" +
                "&language=${encodeURLParam(transcriptionLanguage)}" +
                "&region=${encodeURLParam(transcriptionRegion)}" +
                "&engine=${encodeURLParam(transcribeEngine)}" +
                "&transcriptionStreamParams=${encodeURLParam(transcriptionAdditionalParams)}"
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)

        return if (response.httpException == null) {
            response.data
        } else {
            logger.error(TAG, "Error sending start transcription request ${response.httpException}")
            null
        }
    }
}
