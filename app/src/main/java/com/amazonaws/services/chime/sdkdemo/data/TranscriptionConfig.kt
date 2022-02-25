package com.amazonaws.services.chime.sdkdemo.data

data class TranscribeEngine(val engine: String, val name: String) {
    override fun toString(): String {
        return name
    }
}

data class TranscribeLanguage(val code: String, val name: String) {
    override fun toString(): String {
        return name
    }
}

data class TranscribeRegion(val code: String, val name: String) {
    override fun toString(): String {
        return name
    }
}

data class TranscribeOption(val content: String?, val value: String) {
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
