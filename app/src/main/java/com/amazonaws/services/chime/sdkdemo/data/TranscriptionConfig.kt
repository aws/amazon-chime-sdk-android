package com.amazonaws.services.chime.sdkdemo.data

data class SpinnerItem(val spinnerText: String?, val value: String) {
    override fun toString(): String {
        return value
    }
}

data class TranscriptionStreamParams(
    val contentIdentificationType: String?,
    val contentRedactionType: String?,
    val enablePartialResultsStabilization: Boolean,
    val partialResultsStability: String?,
    val piiEntityTypes: String?,
    val languageModelName: String?
)
