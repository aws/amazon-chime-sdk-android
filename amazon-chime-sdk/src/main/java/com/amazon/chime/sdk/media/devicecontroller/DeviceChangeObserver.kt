package com.amazon.chime.sdk.media.devicecontroller

interface DeviceChangeObserver {
    /**
     *Called when audio devices are changed.
     */
    fun onAudioDeviceChange(freshAudioDeviceList: List<MediaDevice>)
}
