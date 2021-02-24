package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DebugSettingsViewModel : ViewModel() {
    val endpointUrl = MutableLiveData<String>("")

    fun sendEndpointUrl(data: String) {
        endpointUrl.value = data
    }
}
