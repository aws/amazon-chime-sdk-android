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

    companion object {
        private const val TRANSCRIBE_CONTENT_IDENTIFICATION_TYPE = "PII"
        private const val TRANSCRIBE_MEDICAL_CONTENT_IDENTIFICATION_TYPE = "PHI"
        private const val TRANSCRIBE_MEDICAL_ENGINE = "transcribe_medical"
        private const val TRANSCRIBE_PARTIAL_RESULTS_STABILIZATION_DEFAULT = "default"
        private const val TRANSCRIBE_PARTIAL_RESULTS_STABILIZATION_HIGH = "high"
    }

    private val logger = ConsoleLogger(LogLevel.INFO)

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var meetingId: String

    private val TAG = "TranscriptionConfigActivity"

    private lateinit var meetingEndpointUrl: String

    private val gson = Gson()

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
        language: SpinnerItem?,
        region: SpinnerItem,
        partialResultsStability: SpinnerItem,
        contentIdentificationType: SpinnerItem?,
        contentRedactionType: SpinnerItem?,
        languageModelName: String?,
        identifyLanguage: Boolean,
        languageOptions: String?,
        preferredLanguage: SpinnerItem?
    ) {
        uiScope.launch {
            val response: String? =
                enableMeetingTranscription(
                    meetingId,
                    engine.spinnerText,
                    language?.spinnerText,
                    region.spinnerText,
                    partialResultsStability.spinnerText,
                    contentIdentificationType?.spinnerText,
                    contentRedactionType?.spinnerText,
                    languageModelName,
                    identifyLanguage,
                    languageOptions,
                    preferredLanguage?.spinnerText
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
        engine: String?,
        languageCode: String?,
        region: String?,
        transcribePartialResultsStabilization: String?,
        transcribeContentIdentification: String?,
        transcribeContentRedaction: String?,
        customLanguageModel: String?,
        identifyLanguage: Boolean?,
        languageOptions: String?,
        preferredLanguage: String?
    ): String? {
        val partialResultsStabilizationEnabled = transcribePartialResultsStabilization != null
        val isTranscribeMedical = engine.equals(TRANSCRIBE_MEDICAL_ENGINE)
        val transcriptionStreamParams = TranscriptionStreamParams(
            contentIdentificationType = transcribeContentIdentification?.let {
                if (isTranscribeMedical) TRANSCRIBE_MEDICAL_CONTENT_IDENTIFICATION_TYPE
                else TRANSCRIBE_CONTENT_IDENTIFICATION_TYPE
            },
            contentRedactionType = transcribeContentRedaction?.let { TRANSCRIBE_CONTENT_IDENTIFICATION_TYPE },
            enablePartialResultsStabilization = partialResultsStabilizationEnabled,
            partialResultsStability = transcribePartialResultsStabilization?.let {
                if (it == TRANSCRIBE_PARTIAL_RESULTS_STABILIZATION_DEFAULT) TRANSCRIBE_PARTIAL_RESULTS_STABILIZATION_HIGH
                else it
            },
            piiEntityTypes = transcribeContentIdentification?.let { identification ->
                if (identification.isEmpty() || isTranscribeMedical) null
                else identification
            } ?: run { if (transcribeContentRedaction.isNullOrEmpty()) null else transcribeContentRedaction },
            languageModelName = customLanguageModel?.ifEmpty { null },
            identifyLanguage = identifyLanguage?.let { if (it) { true } else null },
            languageOptions = languageOptions?.ifEmpty { null },
            preferredLanguage = preferredLanguage?.ifEmpty { null }
        )
        val transcriptionAdditionalParams = gson.toJson(transcriptionStreamParams)
        val languageCodeParams = if (isTranscribeMedical || (identifyLanguage == false)) {
            "&language=${encodeURLParam(languageCode)}"
        } else ""
        val meetingUrl = if (meetingEndpointUrl.endsWith("/")) meetingEndpointUrl else meetingEndpointUrl.plus("/")
        val url = "${meetingUrl}start_transcription?title=${encodeURLParam(meetingId)}" +
                languageCodeParams +
                "&region=${encodeURLParam(region)}" +
                "&engine=${encodeURLParam(engine)}" +
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
