/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.app.AlertDialog as AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.data.TranscribeEngine
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguage
import com.amazonaws.services.chime.sdkdemo.data.TranscribeOption
import com.amazonaws.services.chime.sdkdemo.data.TranscribeRegion
import java.lang.ClassCastException
import kotlinx.android.synthetic.main.fragment_transcription_config.checkboxCustomLanguageModel
import kotlinx.android.synthetic.main.fragment_transcription_config.checkboxPHIContentIdentification
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

    private val transcribePartialResultStabilizationValues: List<TranscribeOption> = mapOf<String?, String>(
        null to "Enable Partial Results Stabilization",
        "default" to "-- DEFAULT (HIGH) --",
        "low" to "Low",
        "medium" to "Medium",
        "high" to "High"
    ).map {
        TranscribeOption(it.key, it.value)
    }

    private val transcribePiiOptions: List<TranscribeOption> = mapOf<String, String>(
        "" to "ALL",
        "BANK_ROUTING" to "BANK ROUTING",
        "CREDIT_DEBIT_NUMBER" to "CREDIT/DEBIT NUMBER",
        "CREDIT_DEBIT_CVV" to "CREDIT/DEBIT CVV",
        "CREDIT_DEBIT_EXPIRY" to "CREDIT/DEBIT EXPIRY",
        "PIN" to "PIN",
        "EMAIL" to "EMAIL",
        "ADDRESS" to "ADDRESS",
        "NAME" to "NAME",
        "PHONE" to "PHONE NUMBER",
        "SSN" to "SSN"
    ).map {
        TranscribeOption(it.key, it.value)
    }

    private val transcribeIdentificationOptions: MutableList<TranscribeOption> = mutableListOf<TranscribeOption>()
    private val transcribeRedactionOptions: MutableList<TranscribeOption> = mutableListOf<TranscribeOption>()
    private val transcribePartialResultStabilizationOptions: MutableList<TranscribeOption> = transcribePartialResultStabilizationValues.toMutableList()

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
    private lateinit var partialResultsStabilizationSpinner: Spinner
    private lateinit var partialResultsStabilizationAdapter: ArrayAdapter<TranscribeOption>
    private lateinit var piiIdentificationSpinner: Spinner
    private lateinit var piiIdentificationAdapter: ArrayAdapter<TranscribeOption>
    private lateinit var piiRedactionSpinner: Spinner
    private lateinit var piiRedactionAdapter: ArrayAdapter<TranscribeOption>
    private lateinit var phiIdentificationCheckBox: CheckBox
    private lateinit var customLanguageCheckbox: CheckBox

    private val TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY = "transcribeEngineSpinnerIndex"
    private val LANGUAGE_SPINNER_INDEX_KEY = "languageSpinnerIndex"
    private val REGION_SPINNER_INDEX_KEY = "regionSpinnerIndex"
    private val PARTIAL_RESULTS_STABILIZATION_INDEX_KEY = "partialResultsStabilizationSpinnerIndex"
    private val PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY = "piiContentIdentificationSpinnerIndex"
    private val PII_CONTENT_REDACTION_SPINNER_INDEX_KEY = "piiContentRedactionSpinnerIndex"
    private val PHI_CONTENT_IDENTIFICATION_ENABLED_KEY = "phiContentIdentificationEnabled"
    private val CUSTOM_LANGUAGE_MODEL_NAME_KEY = "customLanguageModelName"

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
        fun onStartTranscription(
            engine: TranscribeEngine,
            language: TranscribeLanguage,
            region: TranscribeRegion,
            partialResultsStability: TranscribeOption,
            contentIdentificationType: TranscribeOption,
            contentRedactionType: TranscribeOption,
            languageModelName: String
        )
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
                regionSpinner.selectedItem as TranscribeRegion,
                partialResultsStabilizationSpinner.selectedItem as TranscribeOption,
                piiIdentificationSpinner.selectedItem as TranscribeOption,
                piiRedactionSpinner.selectedItem as TranscribeOption,
                customLanguageCheckbox.text as String
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

        partialResultsStabilizationSpinner = view.findViewById(R.id.spinnerPartialResultsStabilization)
        partialResultsStabilizationAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribePartialResultStabilizationOptions)
        partialResultsStabilizationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        partialResultsStabilizationSpinner.adapter = partialResultsStabilizationAdapter
        partialResultsStabilizationSpinner.isSelected = false
        partialResultsStabilizationSpinner.setSelection(0, true)

        piiIdentificationSpinner = view.findViewById(R.id.spinnerPIIContentIdentification)
        piiIdentificationAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeIdentificationOptions)
        piiIdentificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        piiIdentificationSpinner.adapter = piiIdentificationAdapter
        piiIdentificationSpinner.isSelected = false
        piiIdentificationSpinner.setSelection(0, true)
        piiIdentificationSpinner.onItemSelectedListener = onPIIContentIdentificationSelected

        piiRedactionSpinner = view.findViewById(R.id.spinnerPIIContentRedaction)
        piiRedactionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeRedactionOptions)
        piiRedactionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        piiRedactionSpinner.adapter = piiRedactionAdapter
        piiRedactionSpinner.isSelected = false
        piiRedactionSpinner.setSelection(0, true)
        piiRedactionSpinner.onItemSelectedListener = onPIIContentRedactionSelected

        phiIdentificationCheckBox = view.findViewById(R.id.checkboxPHIContentIdentification)
        phiIdentificationCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                piiIdentificationSpinner.setSelection(1, true)
            }
        }

        customLanguageCheckbox = view.findViewById(R.id.checkboxCustomLanguageModel)
        customLanguageCheckbox.setOnCheckedChangeListener { cb, isChecked ->
            if (isChecked) {
                setCustomLanguageModelName(cb)
            } else {
                checkboxCustomLanguageModel.text = resources.getString(R.string.custom_language_checkbox)
            }
        }

        uiScope.launch {
            populateLanguages(transcribeLanguages, languages, languageAdapter)
            populateRegions(transcribeRegions, regions, regionAdapter)
            populateTranscriptionFilters(transcribePiiOptions, transcribeIdentificationOptions, piiIdentificationAdapter, "Identification")
            populateTranscriptionFilters(transcribePiiOptions, transcribeRedactionOptions, piiRedactionAdapter, "Redaction")

            var transcribeEngineSpinnerIndex = 0
            var languageSpinnerIndex = 0
            var regionSpinnerIndex = 0
            var partialResultsStabilizationSpinnerIndex = 0
            var piiContentIdentificationSpinnerIndex = 0
            var piiContentRedactionSpinnerIndex = 0
            var phiContentIdentificationEnabled = false
            var customLanguageModelName = resources.getString(R.string.custom_language_checkbox)
            if (savedInstanceState != null) {
                transcribeEngineSpinnerIndex = savedInstanceState.getInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, 0)
                languageSpinnerIndex = savedInstanceState.getInt(LANGUAGE_SPINNER_INDEX_KEY, 0)
                regionSpinnerIndex = savedInstanceState.getInt(REGION_SPINNER_INDEX_KEY, 0)
                partialResultsStabilizationSpinnerIndex = savedInstanceState.getInt(PARTIAL_RESULTS_STABILIZATION_INDEX_KEY, 0)
                piiContentIdentificationSpinnerIndex = savedInstanceState.getInt(PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY, 0)
                piiContentRedactionSpinnerIndex = savedInstanceState.getInt(PII_CONTENT_REDACTION_SPINNER_INDEX_KEY, 0)
                phiContentIdentificationEnabled = savedInstanceState.getBoolean(PHI_CONTENT_IDENTIFICATION_ENABLED_KEY, false)
                customLanguageModelName = savedInstanceState.getString(CUSTOM_LANGUAGE_MODEL_NAME_KEY, resources.getString(R.string.custom_language_checkbox))
            }

            transcribeEngineSpinner.setSelection(transcribeEngineSpinnerIndex)
            languageSpinner.setSelection(languageSpinnerIndex)
            regionSpinner.setSelection(regionSpinnerIndex)
            partialResultsStabilizationSpinner.setSelection(partialResultsStabilizationSpinnerIndex)
            piiIdentificationSpinner.setSelection(piiContentIdentificationSpinnerIndex)
            piiRedactionSpinner.setSelection(piiContentRedactionSpinnerIndex)
            checkboxPHIContentIdentification.isChecked = phiContentIdentificationEnabled
            checkboxCustomLanguageModel.isChecked = customLanguageModelName != (resources.getString(R.string.custom_language_checkbox))
            checkboxCustomLanguageModel.text = customLanguageModelName
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, transcribeEngineSpinner.selectedItemPosition)
        outState.putInt(LANGUAGE_SPINNER_INDEX_KEY, languageSpinner.selectedItemPosition)
        outState.putInt(REGION_SPINNER_INDEX_KEY, regionSpinner.selectedItemPosition)
        outState.putInt(PARTIAL_RESULTS_STABILIZATION_INDEX_KEY, partialResultsStabilizationSpinner.selectedItemPosition)
        outState.putInt(PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY, piiIdentificationSpinner.selectedItemPosition)
        outState.putInt(PII_CONTENT_REDACTION_SPINNER_INDEX_KEY, piiRedactionSpinner.selectedItemPosition)
        outState.putBoolean(PHI_CONTENT_IDENTIFICATION_ENABLED_KEY, phiIdentificationCheckBox.isChecked)
        outState.putString(CUSTOM_LANGUAGE_MODEL_NAME_KEY, checkboxCustomLanguageModel.text.toString())
    }

    private val onTranscribeEngineSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeEngines.size) {
                when (transcribeEngines[position].engine) {
                    "transcribe_medical" -> {
                        populateLanguages(transcribeMedicalLanguages, languages, languageAdapter)
                        populateRegions(transcribeMedicalRegions, regions, regionAdapter)
                        hideAdditionalTranscribeOptions()
                        showAdditionalMedicalTranscribeOptions()
                    }
                    "transcribe" -> {
                        populateLanguages(transcribeLanguages, languages, languageAdapter)
                        populateRegions(transcribeRegions, regions, regionAdapter)
                        showAdditionalTranscribeOptions()
                        hideAdditionalMedicalTranscribeOptions()
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

    private val onPIIContentIdentificationSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeIdentificationOptions.size && position > 0) {
                populateTranscriptionFilters(transcribePiiOptions, transcribeRedactionOptions, piiRedactionAdapter, "Redaction")
                piiRedactionSpinner.setSelection(0, true)
            } else {
                logger.error(TAG, "Incorrect position in TranscribeIdentification spinner")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    private val onPIIContentRedactionSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeRedactionOptions.size && position > 0) {
                populateTranscriptionFilters(transcribePiiOptions, transcribeIdentificationOptions, piiIdentificationAdapter, "Identification")
                piiIdentificationSpinner.setSelection(0, true)
            } else {
                logger.error(TAG, "Incorrect position in TranscribeRedaction spinner")
            }
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

    private fun populateTranscriptionFilters(newList: List<TranscribeOption>, currentList: MutableList<TranscribeOption>, adapter: ArrayAdapter<TranscribeOption>, type: String) {
        currentList.clear()
        currentList.addAll(newList)
        currentList.add(0, TranscribeOption(null, "Enable PII Content $type"))
        adapter.notifyDataSetChanged()
    }

    private fun resetSpinner() {
        languageSpinner.setSelection(0, true)
        regionSpinner.setSelection(0, true)
    }

    private fun hideAdditionalTranscribeOptions() {
        // Enable PHI and disable PII when medicalTranscribe selected.
        piiIdentificationSpinner.setSelection(0, true)
        piiRedactionSpinner.setSelection(0, true)
        partialResultsStabilizationSpinner.setSelection(0, true)
        checkboxCustomLanguageModel.text = resources.getString(R.string.custom_language_checkbox)
        piiRedactionSpinner.visibility = View.GONE
        piiIdentificationSpinner.visibility = View.GONE
        checkboxCustomLanguageModel.visibility = View.GONE
        partialResultsStabilizationSpinner.visibility = View.GONE
    }

    private fun showAdditionalTranscribeOptions() {
        // Enable PII and disable PII when transcribe selected.
        piiRedactionSpinner.visibility = View.VISIBLE
        piiIdentificationSpinner.visibility = View.VISIBLE
        partialResultsStabilizationSpinner.visibility = View.VISIBLE
        checkboxCustomLanguageModel.visibility = View.VISIBLE
        checkboxCustomLanguageModel.isChecked = false
    }

    private fun hideAdditionalMedicalTranscribeOptions() {
        phiIdentificationCheckBox.isChecked = false
        piiIdentificationSpinner.setSelection(0, true)
        phiIdentificationCheckBox.visibility = View.GONE
    }

    private fun showAdditionalMedicalTranscribeOptions() {
        phiIdentificationCheckBox.visibility = View.VISIBLE
    }

    private fun setCustomLanguageModelName(cb: CompoundButton) {
        val builder = AlertDialog.Builder(this.context)
        val popUpView = layoutInflater.inflate(R.layout.custom_language_popup, null)
        val customLanguageModelText = popUpView.findViewById<EditText>(R.id.customLanguageModelSetting)
        val saveButton: Button = popUpView.findViewById(R.id.customLanguageSaveButton)
        val cancelButton: Button = popUpView.findViewById(R.id.customLanguageCancelButton)
        builder.setView(popUpView)
        val dialog = builder.create()
        dialog.show()

        saveButton.setOnClickListener {
            if (customLanguageModelText.text.isEmpty()) {
                Toast.makeText(context, "Custom language model name cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                val customLanguageModelName = customLanguageModelText.hint.toString() + ": " + customLanguageModelText.text
                customLanguageCheckbox.text = customLanguageModelName
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            cb.isChecked = false
            dialog.dismiss()
        }
    }
}
