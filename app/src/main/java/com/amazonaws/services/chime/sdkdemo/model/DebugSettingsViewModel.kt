package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DebugSettingsViewModel : ViewModel() {
    val endpointUrl = MutableLiveData("")
    val primaryMeetingId = MutableLiveData("")

    fun sendEndpointUrl(data: String) {
        endpointUrl.value = data
    }

    fun sendPrimaryMeetingId(data: String) {
        primaryMeetingId.value = data
    }
}
