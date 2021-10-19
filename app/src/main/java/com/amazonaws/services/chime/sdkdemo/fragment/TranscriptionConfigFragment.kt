/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.data.TranscribeEngine
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguage
import com.amazonaws.services.chime.sdkdemo.data.TranscribeRegion
import java.lang.ClassCastException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranscriptionConfigFragment : Fragment() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var languages = mutableListOf<TranscribeLanguage>()
    private var regions = mutableListOf<TranscribeRegion>()

    private val languagesMap = mapOf<String, String>(
        "en-US" to "US English (en-US)",
        "es-US" to "US Spanish (es-US)",
        "en-GB" to "British English (en-GB)",
        "en-AU" to "Australian English (en-AU)",
        "fr-CA" to "Canadian French (fr-CA)",
        "fr-FR" to "French (fr-FR)",
        "it-IT" to "Italian (it-IT)",
        "de-DE" to "German (de-DE)",
        "pt-BR" to "Brazilian Portuguese (pt-BR)",
        "ja-JP" to "Japanese (ja-JP)",
        "ko-KR" to "Korean (ko-KR)",
        "zh-CN" to "Mandarin Chinese - Mainland (zh-CN)"
    )

    private val regionsMap = mapOf<String, String>(
        "auto" to "Auto",
        "" to "Not specified",
        "ap-northeast-1" to "Japan (Tokyo)",
        "ap-northeast-2" to "South Korea (Seoul)",
        "ap-southeast-2" to "Australia (Sydney)",
        "ca-central-1" to "Canada",
        "eu-central-1" to "Germany (Frankfurt)",
        "eu-west-1" to "Ireland",
        "eu-west-2" to "United Kingdom (London)",
        "sa-east-1" to "Brazil (São Paulo)",
        "us-east-1" to "United States (N. Virginia)",
        "us-east-2" to "United States (Ohio)",
        "us-west-2" to "United States (Oregon)"
    )

    private val transcribeLanguages: List<TranscribeLanguage> = arrayOf(
        "en-US",
        "es-US",
        "en-GB",
        "en-AU",
        "fr-CA",
        "fr-FR",
        "it-IT",
        "de-DE",
        "pt-BR",
        "ja-JP",
        "ko-KR",
        "zh-CN"
    ).mapNotNull { l ->
        languagesMap[l]?.let {
            TranscribeLanguage(l, it)
        }
    }

    private val transcribeRegions: List<TranscribeRegion> = arrayOf(
        "auto",
        "",
        "ap-northeast-1",
        "ap-northeast-2",
        "ap-southeast-2",
        "ca-central-1",
        "eu-central-1",
        "eu-west-1",
        "eu-west-2",
        "sa-east-1",
        "us-east-1",
        "us-east-2",
        "us-west-2"
    ).mapNotNull { l ->
        regionsMap[l]?.let {
            TranscribeRegion(l, it)
        }
    }

    private val transcribeMedicalLanguages: List<TranscribeLanguage> =
        arrayOf("en-US").mapNotNull { l ->
            languagesMap[l]?.let {
                TranscribeLanguage(l, it)
        }
    }

    private val transcribeMedicalRegions: List<TranscribeRegion> = arrayOf(
        "auto",
        "",
        "ap-southeast-2",
        "ca-central-1",
        "eu-west-1",
        "us-east-1",
        "us-east-2",
        "us-west-2"
    ).mapNotNull { l ->
        regionsMap[l]?.let {
            TranscribeRegion(l, it)
        }
    }

    private val transcribeEngines: List<TranscribeEngine> = mapOf<String, String>(
        "transcribe" to "Amazon Transcribe",
        "transcribe_medical" to "Amazon Transcribe Medical"
    ).map {
        TranscribeEngine(it.key, it.value)
    }

    private lateinit var listener: TranscriptionConfigurationEventListener

    private val TAG = "TranscriptionConfigurationFragment"

    private lateinit var transcribeEngineSpinner: Spinner
    private lateinit var transcribeEngineAdapter: ArrayAdapter<TranscribeEngine>
    private lateinit var languageSpinner: Spinner
    private lateinit var languageAdapter: ArrayAdapter<TranscribeLanguage>
    private lateinit var regionSpinner: Spinner
    private lateinit var regionAdapter: ArrayAdapter<TranscribeRegion>

    private val TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY = "transcribeEngineSpinnerIndex"
    private val LANGUAGE_SPINNER_INDEX_KEY = "languageSpinnerIndex"
    private val REGION_SPINNER_INDEX_KEY = "regionSpinnerIndex"

    companion object {
        fun newInstance(meetingId: String): TranscriptionConfigFragment {
            val fragment = TranscriptionConfigFragment()

            fragment.arguments =
                Bundle().apply {
                    putString(HomeActivity.MEETING_ID_KEY, meetingId)
                }
            return fragment
        }
    }

    interface TranscriptionConfigurationEventListener {
        fun onStartTranscription(engine: TranscribeEngine, language: TranscribeLanguage, region: TranscribeRegion)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is TranscriptionConfigurationEventListener) {
            listener = context
        } else {
            logger.error(TAG, "$context must implement TranscriptionConfigurationEventListener.")
            throw ClassCastException("$context must implement TranscriptionConfigurationEventListener.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transcription_config, container, false)
        val context = activity as Context

        view.findViewById<Button>(R.id.buttonStartTranscription)?.setOnClickListener {
            listener.onStartTranscription(
                transcribeEngineSpinner.selectedItem as TranscribeEngine,
                languageSpinner.selectedItem as TranscribeLanguage,
                regionSpinner.selectedItem as TranscribeRegion
            )
        }

        // Note we call isSelected and setSelection before setting onItemSelectedListener
        // so that we can control the first time the spinner is set and use previous values
        // if they exist (i.e. before rotation). We will set them after lists are populated.

        transcribeEngineSpinner = view.findViewById(R.id.spinnerTranscribeEngine)
        transcribeEngineAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeEngines)
        transcribeEngineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transcribeEngineSpinner.adapter = transcribeEngineAdapter
        transcribeEngineSpinner.isSelected = false
        transcribeEngineSpinner.setSelection(0, true)
        transcribeEngineSpinner.onItemSelectedListener = onTranscribeEngineSelected

        languageSpinner = view.findViewById(R.id.spinnerLanguage)
        languageAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter
        languageSpinner.isSelected = false

        regionSpinner = view.findViewById(R.id.spinnerRegion)
        regionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        regionSpinner.adapter = regionAdapter
        regionSpinner.isSelected = false

        uiScope.launch {
            populateLanguages(transcribeLanguages, languages, languageAdapter)
            populateRegions(transcribeRegions, regions, regionAdapter)

            var transcribeEngineSpinnerIndex = 0
            var languageSpinnerIndex = 0
            var regionSpinnerIndex = 0
            if (savedInstanceState != null) {
                transcribeEngineSpinnerIndex = savedInstanceState.getInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, 0)
                languageSpinnerIndex = savedInstanceState.getInt(LANGUAGE_SPINNER_INDEX_KEY, 0)
                regionSpinnerIndex = savedInstanceState.getInt(REGION_SPINNER_INDEX_KEY, 0)
            }

            transcribeEngineSpinner.setSelection(transcribeEngineSpinnerIndex)
            languageSpinner.setSelection(languageSpinnerIndex)
            regionSpinner.setSelection(regionSpinnerIndex)
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, transcribeEngineSpinner.selectedItemPosition)
        outState.putInt(LANGUAGE_SPINNER_INDEX_KEY, languageSpinner.selectedItemPosition)
        outState.putInt(REGION_SPINNER_INDEX_KEY, regionSpinner.selectedItemPosition)
    }

    private val onTranscribeEngineSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeEngines.size) {
                when (transcribeEngines[position].engine) {
                    "transcribe_medical" -> {
                        populateLanguages(transcribeMedicalLanguages, languages, languageAdapter)
                        populateRegions(transcribeMedicalRegions, regions, regionAdapter)
                    }
                    "transcribe" -> {
                        populateLanguages(transcribeLanguages, languages, languageAdapter)
                        populateRegions(transcribeRegions, regions, regionAdapter)
                    }
                    else -> {
                        logger.error(TAG, "Invalid in TranscribeEngine selected")
                    }
                }
            } else {
                logger.error(TAG, "Incorrect position in TranscribeEngine spinner")
            }
            resetSpinner()
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    private fun populateLanguages(newList: List<TranscribeLanguage>, currentList: MutableList<TranscribeLanguage>, adapter: ArrayAdapter<TranscribeLanguage>) {
        currentList.clear()
        currentList.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    private fun populateRegions(newList: List<TranscribeRegion>, currentList: MutableList<TranscribeRegion>, adapter: ArrayAdapter<TranscribeRegion>) {
        currentList.clear()
        currentList.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    private fun resetSpinner() {
        languageSpinner.setSelection(0, true)
        regionSpinner.setSelection(0, true)
    }
}
