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

data class TranscribePII(val content: String?, val value: String) {
    override fun toString(): String {
        return value
    }
}
