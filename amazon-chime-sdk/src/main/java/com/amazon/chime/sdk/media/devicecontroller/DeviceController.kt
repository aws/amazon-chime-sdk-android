package com.amazon.chime.sdk.media.devicecontroller

interface DeviceController {
    /**
     * Lists currently available audio devices.
     *
     * @return a list of currently available audio devices.
     */
    fun listAudioDevices(): List<MediaDevice>

    /**
     * Selects an audio device to use.
     *
     * @param mediaDevice the audio device selected to use.
     */
    fun chooseAudioDevice(mediaDevice: MediaDevice)

    /**
     * Adds an observer to receive callbacks about device changes.
     */
    fun addDeviceChangeObserver(observer: DeviceChangeObserver)

    /**
     * Removes an observer to stop receiving callbacks about device changes.
     */
    fun removeDeviceChangeObserver(observer: DeviceChangeObserver)
}
