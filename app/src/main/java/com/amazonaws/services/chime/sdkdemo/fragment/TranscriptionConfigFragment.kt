/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.adapter.LanguageOptionsAdapter
import com.amazonaws.services.chime.sdkdemo.data.SpinnerItem
import com.amazonaws.services.chime.sdkdemo.data.TranscribeLanguageOption
import java.lang.ClassCastException
import kotlinx.android.synthetic.main.fragment_transcription_config.checkboxCustomLanguageModel
import kotlinx.android.synthetic.main.fragment_transcription_config.checkboxPHIContentIdentification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranscriptionConfigFragment : Fragment() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var languages = mutableListOf<SpinnerItem>()
    private var regions = mutableListOf<SpinnerItem>()

    private val languageGroups = arrayListOf<String>(
        "English",
        "Spanish",
        "French",
        "Italian",
        "German",
        "Portuguese",
        "Japanese",
        "Korean",
        "Chinese"
    )

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

    private val languageGroupMapping: List<Pair<String, String>> = listOf<Pair<String, String>>(
        Pair("English", "en-US"),
        Pair("English", "en-GB"),
        Pair("English", "en-AU"),
        Pair("Spanish", "es-US"),
        Pair("French", "fr-CA"),
        Pair("French", "fr-FR"),
        Pair("Italian", "it-IT"),
        Pair("German", "de-DE"),
        Pair("Portuguese", "pt-BR"),
        Pair("Japanese", "ja-JP"),
        Pair("Korean", "ko-KR"),
        Pair("Chinese", "zh-CN")
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

    private val transcribeLanguages: List<SpinnerItem> = arrayOf(
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
            SpinnerItem(l, it)
        }
    }

    private val transcribeRegions: List<SpinnerItem> = arrayOf(
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
            SpinnerItem(l, it)
        }
    }

    private val transcribePartialResultStabilizationValues: List<SpinnerItem> = mapOf<String?, String>(
        null to "Enable Partial Results Stabilization",
        "default" to "-- DEFAULT (HIGH) --",
        "low" to "Low",
        "medium" to "Medium",
        "high" to "High"
    ).map {
        SpinnerItem(it.key, it.value)
    }

    private val transcribePiiOptions: List<SpinnerItem> = mapOf<String, String>(
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
        SpinnerItem(it.key, it.value)
    }

    private val transcribePreferredLanguageDefaultOption: List<SpinnerItem> = mapOf<String, String>(
        "" to "[Optional] Select preferred language"
    ).map {
        SpinnerItem(it.key, it.value)
    }

    private val transcribeIdentificationOptions: MutableList<SpinnerItem> = mutableListOf<SpinnerItem>()
    private val transcribeRedactionOptions: MutableList<SpinnerItem> = mutableListOf<SpinnerItem>()
    private val transcribePartialResultStabilizationOptions: MutableList<SpinnerItem> = transcribePartialResultStabilizationValues.toMutableList()
    private val preferredLanguageOptions: MutableList<SpinnerItem> = mutableListOf<SpinnerItem>()
    private val languageOptionsSelected: MutableSet<TranscribeLanguageOption> = mutableSetOf<TranscribeLanguageOption>()
    private val languageOptions: HashMap<String, MutableList<SpinnerItem?>> = HashMap()

    private val transcribeMedicalLanguages: List<SpinnerItem> =
        arrayOf("en-US").mapNotNull { l ->
            languagesMap[l]?.let {
                SpinnerItem(l, it)
        }
    }

    private val transcribeMedicalRegions: List<SpinnerItem> = arrayOf(
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
            SpinnerItem(l, it)
        }
    }

    private val transcribeEngines: List<SpinnerItem> = mapOf<String, String>(
        "transcribe" to "Amazon Transcribe",
        "transcribe_medical" to "Amazon Transcribe Medical"
    ).map {
        SpinnerItem(it.key, it.value)
    }

    private lateinit var listener: TranscriptionConfigurationEventListener

    private val TAG = "TranscriptionConfigurationFragment"

    private lateinit var transcribeEngineSpinner: Spinner
    private lateinit var transcribeEngineAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var languageSpinner: Spinner
    private lateinit var languageAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var regionSpinner: Spinner
    private lateinit var regionAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var partialResultsStabilizationSpinner: Spinner
    private lateinit var partialResultsStabilizationAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var piiIdentificationSpinner: Spinner
    private lateinit var piiIdentificationAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var piiRedactionSpinner: Spinner
    private lateinit var piiRedactionAdapter: ArrayAdapter<SpinnerItem>
    private lateinit var phiIdentificationCheckBox: CheckBox
    private lateinit var customLanguageModelCheckbox: CheckBox
    private lateinit var customLanguageModelEditText: EditText
    private lateinit var identifyLanguageCheckbox: CheckBox
    private lateinit var languageOptionsListView: ExpandableListView
    private lateinit var languageOptionsTextView: TextView
    private lateinit var languageOptionsAdapter: ExpandableListAdapter
    private lateinit var preferredLanguageSpinner: Spinner
    private lateinit var preferredLanguageAdapter: ArrayAdapter<SpinnerItem>

    private val TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY = "transcribeEngineSpinnerIndex"
    private val LANGUAGE_SPINNER_INDEX_KEY = "languageSpinnerIndex"
    private val REGION_SPINNER_INDEX_KEY = "regionSpinnerIndex"
    private val PARTIAL_RESULTS_STABILIZATION_INDEX_KEY = "partialResultsStabilizationSpinnerIndex"
    private val PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY = "piiContentIdentificationSpinnerIndex"
    private val PII_CONTENT_REDACTION_SPINNER_INDEX_KEY = "piiContentRedactionSpinnerIndex"
    private val PHI_CONTENT_IDENTIFICATION_ENABLED_KEY = "phiContentIdentificationEnabled"
    private val CUSTOM_LANGUAGE_MODEL_NAME_KEY = "customLanguageModelName"
    private val IDENTIFY_LANGUAGE_ENABLED_KEY = "identifyLanguageEnabled"
    private val PREFERRED_LANGUAGE_INDEX_KEY = "preferredLanguageIndex"

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
                transcribeEngineSpinner.selectedItem as SpinnerItem,
                languageSpinner.selectedItem?.let {
                    languageSpinner.selectedItem as SpinnerItem },
                regionSpinner.selectedItem as SpinnerItem,
                partialResultsStabilizationSpinner.selectedItem as SpinnerItem,
                if (piiIdentificationSpinner.isEnabled) {
                    piiIdentificationSpinner.selectedItem as SpinnerItem } else null,
                if (piiRedactionSpinner.isEnabled) {
                    piiRedactionSpinner.selectedItem as SpinnerItem } else null,
                if (customLanguageModelCheckbox.isEnabled) {
                    customLanguageModelEditText.text.toString() } else null,
                identifyLanguageCheckbox.isChecked,
                if (identifyLanguageCheckbox.isChecked) {
                    formatLanguageOptions() } else null,
                if (identifyLanguageCheckbox.isChecked) {
                    preferredLanguageSpinner.selectedItem?.let {
                        preferredLanguageSpinner.selectedItem as SpinnerItem } } else null
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

        identifyLanguageCheckbox = view.findViewById(R.id.checkboxIdentifyLanguage)
        languageOptionsTextView = view.findViewById(R.id.textViewLanguageOptions)
        identifyLanguageCheckbox.setOnCheckedChangeListener { languageOptionDialogBox, isChecked ->
            if (isChecked) {
                showLanguageOptionsAlertDialog(languageOptionDialogBox)
                languageSpinner.isEnabled = false
                piiIdentificationSpinner.isEnabled = false
                piiRedactionSpinner.isEnabled = false
                phiIdentificationCheckBox.isEnabled = false
                preferredLanguageSpinner.visibility = View.VISIBLE
                languageOptionsTextView.visibility = View.VISIBLE
                customLanguageModelCheckbox.isEnabled = false
                customLanguageModelEditText.isEnabled = false
                customLanguageModelEditText.visibility = View.GONE
            } else {
                preferredLanguageSpinner.visibility = View.GONE
                languageOptionsTextView.isEnabled = false
                languageSpinner.isEnabled = true
                piiIdentificationSpinner.isEnabled = true
                piiRedactionSpinner.isEnabled = true
                languageOptionsTextView.visibility = View.GONE
                customLanguageModelCheckbox.isEnabled = true
                customLanguageModelEditText.isEnabled = true
                customLanguageModelEditText.visibility = View.VISIBLE
            }
        }

        preferredLanguageSpinner = view.findViewById(R.id.spinnerPreferredLanguage)
        preferredLanguageAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, preferredLanguageOptions)
        preferredLanguageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        preferredLanguageSpinner.adapter = preferredLanguageAdapter
        preferredLanguageSpinner.isSelected = false

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
            } else {
                piiIdentificationSpinner.setSelection(0, true)
            }
        }

        customLanguageModelEditText = view.findViewById(R.id.editTextCustomLanguageModel)

        customLanguageModelCheckbox = view.findViewById(R.id.checkboxCustomLanguageModel)
        // Show / Hide EditText field for custom language model when checkbox clicked.
        customLanguageModelCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                customLanguageModelEditText.visibility = View.VISIBLE
            } else {
                customLanguageModelEditText.visibility = View.GONE
            }
        }

        uiScope.launch {
            populateLanguages(transcribeLanguages, languages, languageAdapter)
            populateRegions(transcribeRegions, regions, regionAdapter)
            populateTranscriptionOptions(transcribePiiOptions, transcribeIdentificationOptions, piiIdentificationAdapter, "Identification")
            populateTranscriptionOptions(transcribePiiOptions, transcribeRedactionOptions, piiRedactionAdapter, "Redaction")
            populateLanguageOptions(languageOptions)

            var transcribeEngineSpinnerIndex = 0
            var languageSpinnerIndex = 0
            var regionSpinnerIndex = 0
            var partialResultsStabilizationSpinnerIndex = 0
            var piiContentIdentificationSpinnerIndex = 0
            var piiContentRedactionSpinnerIndex = 0
            var phiContentIdentificationEnabled = false
            var customLanguageModelName = resources.getString(R.string.custom_language_checkbox)
            var preferredLanguageSpinnerIndex = 0
            var identifyLanguageEnabled = false
            if (savedInstanceState != null) {
                transcribeEngineSpinnerIndex = savedInstanceState.getInt(TRANSCRIBE_ENGINE_SPINNER_INDEX_KEY, 0)
                languageSpinnerIndex = savedInstanceState.getInt(LANGUAGE_SPINNER_INDEX_KEY, 0)
                regionSpinnerIndex = savedInstanceState.getInt(REGION_SPINNER_INDEX_KEY, 0)
                partialResultsStabilizationSpinnerIndex = savedInstanceState.getInt(PARTIAL_RESULTS_STABILIZATION_INDEX_KEY, 0)
                piiContentIdentificationSpinnerIndex = savedInstanceState.getInt(PII_CONTENT_IDENTIFICATION_SPINNER_INDEX_KEY, 0)
                piiContentRedactionSpinnerIndex = savedInstanceState.getInt(PII_CONTENT_REDACTION_SPINNER_INDEX_KEY, 0)
                phiContentIdentificationEnabled = savedInstanceState.getBoolean(PHI_CONTENT_IDENTIFICATION_ENABLED_KEY, false)
                customLanguageModelName = savedInstanceState.getString(CUSTOM_LANGUAGE_MODEL_NAME_KEY, resources.getString(R.string.custom_language_checkbox))
                identifyLanguageEnabled = savedInstanceState.getBoolean(IDENTIFY_LANGUAGE_ENABLED_KEY, false)
                preferredLanguageSpinnerIndex = savedInstanceState.getInt(PREFERRED_LANGUAGE_INDEX_KEY, 0)
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
            identifyLanguageCheckbox.isChecked = identifyLanguageEnabled
            preferredLanguageSpinner.setSelection(preferredLanguageSpinnerIndex)
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
        outState.putInt(PREFERRED_LANGUAGE_INDEX_KEY, preferredLanguageSpinner.selectedItemPosition)
        outState.putBoolean(IDENTIFY_LANGUAGE_ENABLED_KEY, identifyLanguageCheckbox.isChecked)
    }

    private val onTranscribeEngineSelected = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeEngines.size) {
                populateLanguages(transcribeMedicalLanguages, languages, languageAdapter)
                populateRegions(transcribeMedicalRegions, regions, regionAdapter)
                when (transcribeEngines[position].spinnerText) {
                    "transcribe_medical" -> {
                        displayAdditionalTranscriptionOptions(true)
                    }
                    "transcribe" -> {
                        displayAdditionalTranscriptionOptions(false)
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

    // Reset the redaction spinner when content identification selected.
    private val onPIIContentIdentificationSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeIdentificationOptions.size && position > 0) {
                populateTranscriptionOptions(transcribePiiOptions, transcribeRedactionOptions, piiRedactionAdapter, "Redaction")
                piiRedactionSpinner.setSelection(0, true)
            } else {
                logger.error(TAG, "Incorrect position in TranscribeIdentification spinner")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    // Reset the identification spinner when content redaction selected.
    private val onPIIContentRedactionSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position < transcribeRedactionOptions.size && position > 0) {
                populateTranscriptionOptions(transcribePiiOptions, transcribeIdentificationOptions, piiIdentificationAdapter, "Identification")
                piiIdentificationSpinner.setSelection(0, true)
            } else {
                logger.error(TAG, "Incorrect position in TranscribeRedaction spinner")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    private fun populateLanguages(newList: List<SpinnerItem>, currentList: MutableList<SpinnerItem>, adapter: ArrayAdapter<SpinnerItem>) {
        currentList.clear()
        currentList.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    private fun populateRegions(newList: List<SpinnerItem>, currentList: MutableList<SpinnerItem>, adapter: ArrayAdapter<SpinnerItem>) {
        currentList.clear()
        currentList.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    // Populate content identification / redaction spinners and set the label of the spinner with the given 'type' (E.G identification or redaction).
    private fun populateTranscriptionOptions(newList: List<SpinnerItem>, currentList: MutableList<SpinnerItem>, adapter: ArrayAdapter<SpinnerItem>, type: String) {
        currentList.clear()
        currentList.addAll(newList)
        currentList.add(0, SpinnerItem(null, "Enable PII Content $type"))
        adapter.notifyDataSetChanged()
    }

    private fun populateLanguageOptions(languageOptions: HashMap<String, MutableList<SpinnerItem?>>) {
        for ((locale, variant) in languageGroupMapping) {
            if (languageOptions.containsKey(locale)) {
                languageOptions[locale]?.add(languagesMap[variant]?.let {
                    SpinnerItem(variant, it) })
            } else {
                languageOptions[locale] =
                    mutableListOf(languagesMap[variant]?.let { SpinnerItem(variant, it) })
            }
        }
    }

    private fun populatePreferredLanguage(newList: List<SpinnerItem>, currentList: MutableList<SpinnerItem>, adapter: ArrayAdapter<SpinnerItem>) {
        currentList.clear()
        currentList.add(transcribePreferredLanguageDefaultOption[0])
        currentList.addAll(newList)
        adapter.notifyDataSetChanged()
    }

    private fun resetSpinner() {
        languageSpinner.setSelection(0, true)
        regionSpinner.setSelection(0, true)
    }

    // Enable / Disable additional transcription options based on engine selected.
    private fun displayAdditionalTranscriptionOptions(isTranscribeMedical: Boolean) {
        identifyLanguageCheckbox.isChecked = false
        piiIdentificationSpinner.setSelection(0, true)
        piiRedactionSpinner.setSelection(0, true)
        partialResultsStabilizationSpinner.setSelection(0, true)
        checkboxCustomLanguageModel.isChecked = false
        phiIdentificationCheckBox.isChecked = false
        piiIdentificationSpinner.setSelection(0, true)
        checkboxCustomLanguageModel.text = resources.getString(R.string.custom_language_checkbox)
        customLanguageModelEditText.visibility = View.GONE

        piiRedactionSpinner.visibility = if (isTranscribeMedical) View.GONE else View.VISIBLE
        piiIdentificationSpinner.visibility = if (isTranscribeMedical) View.GONE else View.VISIBLE
        checkboxCustomLanguageModel.visibility = if (isTranscribeMedical) View.GONE else View.VISIBLE
        partialResultsStabilizationSpinner.visibility = if (isTranscribeMedical) View.GONE else View.VISIBLE
        phiIdentificationCheckBox.visibility = if (isTranscribeMedical) View.VISIBLE else View.GONE
        identifyLanguageCheckbox.visibility = if (isTranscribeMedical) View.GONE else View.VISIBLE
        languageSpinner.isEnabled = isTranscribeMedical
        phiIdentificationCheckBox.isEnabled = true
        if (isTranscribeMedical) {
            languageOptionsTextView.visibility = View.GONE
            preferredLanguageSpinner.visibility = View.GONE
        }
    }

    private fun showLanguageOptionsAlertDialog(languageOptionDialogBox: CompoundButton) {
        val builder = AlertDialog.Builder(this.context)
        val languageOptionsAlertDialog =
            layoutInflater.inflate(R.layout.alert_dialog_language_options, null)
        languageOptionsListView = languageOptionsAlertDialog.findViewById<ExpandableListView>(R.id.expandableListViewLanguageOptions)

        languageOptionsAdapter = LanguageOptionsAdapter(this.requireContext(), languageOptions, languageGroups, languageOptionsSelected)
        languageOptionsListView.setAdapter(languageOptionsAdapter)
        languageOptionsListView.setOnChildClickListener(OnChildClickListener { _, view, groupPosition, childPosition, _ ->

            val languageOptionCheck: CheckedTextView = view.findViewById(R.id.checkedTextViewLanguageOptionRow)
            languageOptionCheck.toggle()
            val languageSelected: SpinnerItem? = languageOptions[languageGroups[groupPosition]]?.get(childPosition)
            val selectedCell: TranscribeLanguageOption? =
                languageSelected?.let { TranscribeLanguageOption(groupPosition, childPosition, it) }
            selectedCell?.let {
                if (languageOptionsSelected.contains(selectedCell)) {
                    languageOptionsSelected.remove(selectedCell)
                } else {
                    languageOptionsSelected.add(selectedCell)
                }
            }

            validateLanguageOptions(
                languageGroups,
                languageOptionsSelected,
                languageOptionsAlertDialog
            )

            return@OnChildClickListener false
        })

        expandLanguageOptionsGroup()

        builder.setView(languageOptionsAlertDialog)
        val alertDialog = builder.create()
        alertDialog.show()

        val saveButton: Button = languageOptionsAlertDialog.findViewById(R.id.buttonSaveLanguageOptions)
        saveButton.setOnClickListener {
            if (validateLanguageOptions(languageGroups, languageOptionsSelected, languageOptionsAlertDialog)) {
                val languageOptionsSelected: List<SpinnerItem> =
                    languageOptionsSelected.map { languageOptionSelected -> languageOptionSelected.transcribeLanguage }
                populatePreferredLanguage(languageOptionsSelected, preferredLanguageOptions, preferredLanguageAdapter)
                languageOptionsTextView.text = String.format("%s %s", getString(R.string.language_options_text_title),
                    languageOptionsSelected.joinToString(", "))

                alertDialog.dismiss()
            }
        }

        val cancelButton: Button = languageOptionsAlertDialog.findViewById(R.id.buttonCancelLanguageOptions)
        cancelButton.setOnClickListener {
            languageOptionDialogBox.isChecked = false
            alertDialog.dismiss()
        }
    }

    private fun expandLanguageOptionsGroup() {
        for (i in 0 until languageOptionsAdapter.groupCount) {
            languageOptionsListView.expandGroup(i)
        }
    }

    private fun formatLanguageOptions(): String {
        return if (languageOptionsSelected.isEmpty()) "" else
            languageOptionsSelected.joinToString(",") {
                    languageOptionSelected -> languageOptionSelected.transcribeLanguage.spinnerText.toString()
            }
    }

    private fun validateLanguageOptions(
        languageLocales: ArrayList<String>,
        languageOptionsSelectedSet: MutableSet<TranscribeLanguageOption>,
        view: View
    ): Boolean {
        var isValid = false
        val duplicateLanguageLocale: HashSet<String> = HashSet<String>()
        val languageLocaleSelectedSet: HashSet<Int> = HashSet<Int>()
        for (languageOptionSelected in languageOptionsSelectedSet) {
            if (languageLocaleSelectedSet.contains(languageOptionSelected.languageGroupIndex) &&
                !duplicateLanguageLocale.contains(languageLocales[languageOptionSelected.languageGroupIndex])) {
                duplicateLanguageLocale.add(languageLocales[languageOptionSelected.languageGroupIndex])
            } else {
                languageLocaleSelectedSet.add(languageOptionSelected.languageGroupIndex)
            }
        }

        val displayErrorMessageText: String = when {
            duplicateLanguageLocale.size > 0 -> {
                getString(R.string.user_notification_language_option_invalid_selection,
                    duplicateLanguageLocale.joinToString(", "))
            }
            languageLocaleSelectedSet.size < 2 -> {
                getString(R.string.user_notification_language_option_missing_selection)
            }
            else -> {
                isValid = true
                ""
            }
        }

        view.findViewById<TextView>(R.id.textViewErrorLanguageOptions).text = displayErrorMessageText
        return isValid
    }
}
