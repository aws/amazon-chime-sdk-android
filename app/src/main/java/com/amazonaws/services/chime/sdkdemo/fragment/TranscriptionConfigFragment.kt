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
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TransciptPIIValues
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.data.TranscribeEngine
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguage
import com.amazonaws.services.chime.sdkdemo.data.TranscribePII
import com.amazonaws.services.chime.sdkdemo.data.TranscribeRegion
import java.lang.ClassCastException
import kotlinx.android.synthetic.main.fragment_transcription_config.checkboxCustomLanguageModel
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

    private val transcribeParity: List<TranscribePII> = mapOf<String?, String>(
        null to "Partial Stability",
        "low" to "Low",
        "medium" to "Medium",
        "high" to "High"
    ).map {
        TranscribePII(it.key, it.value)
    }

    private val transcribePIIContents: List<TranscribePII> = mapOf<String, String>(
        "" to "ALL",
        TransciptPIIValues.BankRouting.value to "BANK ROUTING",
        TransciptPIIValues.CreditCardNumber.value to "CREDIT/DEBIT NUMBER",
        TransciptPIIValues.CreditCardCVV.value to "CREDIT/DEBIT CVV",
        TransciptPIIValues.CreditCardExpiry.value to "CREDIT/DEBIT EXPIRY",
        TransciptPIIValues.PIN.value to "PIN",
        TransciptPIIValues.EMAIL.value to "EMAIL",
        TransciptPIIValues.ADDRESS.value to "ADDRESS",
        TransciptPIIValues.NAME.value to "NAME",
        TransciptPIIValues.PHONE.value to "PHONE NUMBER",
        TransciptPIIValues.SSN.value to "SSN"
    ).map {
        TranscribePII(it.key, it.value)
    }

    private val transcribeIdentificationContents: MutableList<TranscribePII> = mutableListOf<TranscribePII>()
    private val transcribeRedactionContents: MutableList<TranscribePII> = mutableListOf<TranscribePII>()

    private val transcribeParities: MutableList<TranscribePII> = transcribeParity.toMutableList()

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
    private lateinit var paritySpinner: Spinner
    private lateinit var parityAdapter: ArrayAdapter<TranscribePII>
    private lateinit var piiContentIdentificationSpinner: Spinner
    private lateinit var piiContentIdentificationAdapter: ArrayAdapter<TranscribePII>
    private lateinit var piiContentRedactionSpinner: Spinner
    private lateinit var piiContentRedactionAdapter: ArrayAdapter<TranscribePII>
    private lateinit var customLanguageCheckbox: CheckBox

    private val TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY = "transcribeEngineSpinnerIndex"
    private val LANGUAGE_SPINNER_INDEX_KEY = "languageSpinnerIndex"
    private val REGION_SPINNER_INDEX_KEY = "regionSpinnerIndex"
    private val PARITY_SPINNER_INDEX_KEY = "paritySpinnerIndex"
    private val PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY = "piiContentIdentificationSpinnerIndex"
    private val PII_CONTENT_REDACTION_SPINNER_INDEX_KEY = "piiContentRedactionSpinnerIndex"

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
            transcribeParity: TranscribePII,
            transcribePIIIdentication: TranscribePII,
            transcribePIIRedaction: TranscribePII,
            customLanguageModel: String
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
                paritySpinner.selectedItem as TranscribePII,
                piiContentIdentificationSpinner.selectedItem as TranscribePII,
                piiContentRedactionSpinner.selectedItem as TranscribePII,
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

        paritySpinner = view.findViewById(R.id.spinnerParity)
        parityAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeParities)
        parityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paritySpinner.adapter = parityAdapter
        paritySpinner.isSelected = false
        paritySpinner.setSelection(0, true)

        piiContentIdentificationSpinner = view.findViewById(R.id.spinnerPIIContentIdentification)
        piiContentIdentificationAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeIdentificationContents)
        piiContentIdentificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        piiContentIdentificationSpinner.adapter = piiContentIdentificationAdapter
        piiContentIdentificationSpinner.isSelected = false
        piiContentIdentificationSpinner.setSelection(0, true)
        piiContentIdentificationSpinner.onItemSelectedListener = onPIIContentIdentificationSelected

        piiContentRedactionSpinner = view.findViewById(R.id.spinnerPIIContentRedaction)
        piiContentRedactionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, transcribeRedactionContents)
        piiContentRedactionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        piiContentRedactionSpinner.adapter = piiContentRedactionAdapter
        piiContentRedactionSpinner.isSelected = false
        piiContentRedactionSpinner.setSelection(0, true)
        piiContentRedactionSpinner.onItemSelectedListener = onPIIContentRedactionSelected

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
            populatePII(transcribePIIContents, transcribeIdentificationContents, piiContentIdentificationAdapter, "Identification")
            populatePII(transcribePIIContents, transcribeRedactionContents, piiContentRedactionAdapter, "Redaction")

            var transcribeEngineSpinnerIndex = 0
            var languageSpinnerIndex = 0
            var regionSpinnerIndex = 0
            var paritySpinnerIndex = 0
            var piiContentIdentificationSpinnerIndex = 0
            var piiContentRedactionSpinnerIndex = 0
            if (savedInstanceState != null) {
                transcribeEngineSpinnerIndex = savedInstanceState.getInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, 0)
                languageSpinnerIndex = savedInstanceState.getInt(LANGUAGE_SPINNER_INDEX_KEY, 0)
                regionSpinnerIndex = savedInstanceState.getInt(REGION_SPINNER_INDEX_KEY, 0)
                paritySpinnerIndex = savedInstanceState.getInt(PARITY_SPINNER_INDEX_KEY, 0)
                piiContentIdentificationSpinnerIndex = savedInstanceState.getInt(
                    PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY, 0)
                piiContentRedactionSpinnerIndex = savedInstanceState.getInt(
                    PII_CONTENT_REDACTION_SPINNER_INDEX_KEY, 0)
            }

            transcribeEngineSpinner.setSelection(transcribeEngineSpinnerIndex)
            languageSpinner.setSelection(languageSpinnerIndex)
            regionSpinner.setSelection(regionSpinnerIndex)
            paritySpinner.setSelection(paritySpinnerIndex)
            piiContentIdentificationSpinner.setSelection(piiContentIdentificationSpinnerIndex)
            piiContentRedactionSpinner.setSelection(piiContentRedactionSpinnerIndex)
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, transcribeEngineSpinner.selectedItemPosition)
        outState.putInt(LANGUAGE_SPINNER_INDEX_KEY, languageSpinner.selectedItemPosition)
        outState.putInt(REGION_SPINNER_INDEX_KEY, regionSpinner.selectedItemPosition)
        outState.putInt(PARITY_SPINNER_INDEX_KEY, paritySpinner.selectedItemPosition)
        outState.putInt(PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY, piiContentIdentificationSpinner.selectedItemPosition)
        outState.putInt(PII_CONTENT_REDACTION_SPINNER_INDEX_KEY, piiContentRedactionSpinner.selectedItemPosition)
    }

    private val onTranscribeEngineSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeEngines.size) {
                when (transcribeEngines[position].engine) {
                    "transcribe_medical" -> {
                        populateLanguages(transcribeMedicalLanguages, languages, languageAdapter)
                        populateRegions(transcribeMedicalRegions, regions, regionAdapter)
                        hideClmPii()
                    }
                    "transcribe" -> {
                        populateLanguages(transcribeLanguages, languages, languageAdapter)
                        populateRegions(transcribeRegions, regions, regionAdapter)
                        showClmPii()
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
            if (position < transcribeIdentificationContents.size && position > 0) {
                populatePII(transcribePIIContents, transcribeRedactionContents, piiContentRedactionAdapter, "Redaction")
                piiContentRedactionSpinner.setSelection(0, true)
            } else {
                logger.error(TAG, "Incorrect position in TranscribeIdentification spinner")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    private val onPIIContentRedactionSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeRedactionContents.size && position > 0) {
                populatePII(transcribePIIContents, transcribeIdentificationContents, piiContentIdentificationAdapter, "Identification")
                piiContentIdentificationSpinner.setSelection(0, true)
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

    private fun populatePII(newList: List<TranscribePII>, currentList: MutableList<TranscribePII>, adapter: ArrayAdapter<TranscribePII>, type: String) {
        currentList.clear()
        currentList.addAll(newList)
        currentList.add(0, TranscribePII(null, "Enable PII Content $type"))
        adapter.notifyDataSetChanged()
    }

    private fun resetSpinner() {
        languageSpinner.setSelection(0, true)
        regionSpinner.setSelection(0, true)
    }

    private fun hideClmPii() {
        piiContentIdentificationSpinner.setSelection(0, true)
        piiContentRedactionSpinner.setSelection(0, true)
        paritySpinner.setSelection(0, true)
        checkboxCustomLanguageModel.text = resources.getString(R.string.custom_language_checkbox)
        piiContentRedactionSpinner.visibility = View.GONE
        piiContentIdentificationSpinner.visibility = View.GONE
        checkboxCustomLanguageModel.visibility = View.GONE
        paritySpinner.visibility = View.GONE
    }

    private fun showClmPii() {
        piiContentRedactionSpinner.visibility = View.VISIBLE
        piiContentIdentificationSpinner.visibility = View.VISIBLE
        paritySpinner.visibility = View.VISIBLE
        checkboxCustomLanguageModel.visibility = View.VISIBLE
        checkboxCustomLanguageModel.isChecked = false
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
            val customLanguageModelName = customLanguageModelText.hint.toString() + ": " + customLanguageModelText.text
            customLanguageCheckbox.text = customLanguageModelName
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            cb.isChecked = false
            dialog.dismiss()
        }
    }
}
