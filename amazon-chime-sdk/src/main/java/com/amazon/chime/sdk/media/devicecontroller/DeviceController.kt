package com.amazon.chime.sdk.media.devicecontroller

interface DeviceController {
    /**
     * Lists currently available audio input devices.
     *
     * @return a list of currently available audio input devices.
     */
    fun listAudioInputDevices(): List<MediaDevice>

    /**
     * Lists currently available audio output devices.
     *
     * @return a list of currently available audio output devices.
     */
    fun listAudioOutputDevices(): List<MediaDevice>

    /**
     * Selects an audio input device to use.
     *
     * @param mediaDevice the audio input device selected to use.
     */
    fun chooseAudioInputDevice(mediaDevice: MediaDevice)

    /**
     * Selects an video input device to use.
     *
     * @param mediaDevice the audio output device selected to use.
     */
    fun chooseAudioOutputDevice(mediaDevice: MediaDevice)
}
