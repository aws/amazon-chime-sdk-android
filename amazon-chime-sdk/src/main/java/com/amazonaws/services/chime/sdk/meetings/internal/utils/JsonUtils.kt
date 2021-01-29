package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.google.gson.Gson

class JsonUtils {
    companion object {
        private val gson = Gson()

        fun marshal(data: Any): String {
            return gson.toJson(data)
        }
    }
}
