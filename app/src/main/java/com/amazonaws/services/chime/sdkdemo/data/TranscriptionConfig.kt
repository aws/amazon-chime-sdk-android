package com.amazonaws.services.chime.sdkdemo.data

data class SpinnerItem(val spinnerText: String?, val value: String) {
    override fun toString(): String {
        return value
    }
}

data class TranscribeLanguageOption(
    val languageGroupIndex: Int,
    val languageCodeIndex: Int,
    val transcribeLanguage: SpinnerItem
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranscribeLanguageOption

        if (languageGroupIndex != other.languageGroupIndex && languageCodeIndex != other.languageCodeIndex)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = languageGroupIndex
        result = 31 * result + languageCodeIndex
        return result
    }
}

data class TranscriptionStreamParams(
    val contentIdentificationType: String?,
    val contentRedactionType: String?,
    val enablePartialResultsStabilization: Boolean,
    val partialResultsStability: String?,
    val piiEntityTypes: String?,
    val languageModelName: String?,
    val identifyLanguage: Boolean?,
    val languageOptions: String?,
    val preferredLanguage: String?
)
