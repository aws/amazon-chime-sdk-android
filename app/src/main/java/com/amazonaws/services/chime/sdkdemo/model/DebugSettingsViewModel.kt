package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DebugSettingsViewModel : ViewModel() {
    val endpointUrl = MutableLiveData("")
    val primaryMeetingId = MutableLiveData("")
    val customPort = MutableLiveData("")

    fun sendEndpointUrl(data: String) {
        endpointUrl.value = data
    }

    fun sendPrimaryMeetingId(data: String) {
        primaryMeetingId.value = data
    }

    fun sendCustomPort(data: String) {
        customPort.value = data
    }
}
