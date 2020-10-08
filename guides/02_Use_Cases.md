# Use Cases

This guide gives an overview of the API methods you can use to create meeting with audio and video with a roster of attendees and basic controls. Several additional API methods that may be helpful are also described and marked optional.

## Display currently used audio device from list
You can use `getActiveAudioDevice` in order to achieve display of currently active audio device. For instance, if you see `DeviceAdapter` in our demo app over API 24 (Android 7.0+)
```kt
if (Build.VERSION.SDK_INT >= AUDIO_RECORDING_CONFIG_API_LEVEL) {
    view.text = if (currentDevice == devices[position]) "${devices[position]} ✓" else devices[position].toString()
}
```

In order to support from Android API 21 - 23 (5.0 - 6.0), we need to manually handle cases for connection/reconnection. Since there is optional API `onChooseAudioDeviceCalled` which is called when chooseAudioDevice is called. This will let you make less mistakes. You can set your current device.
```
    override fun onChooseAudioDeviceCalled(device: MediaDevice) {
        audioDeviceManager.setCurrentAudioDevice(device) // update your current device
    }
```

However, you'll still need to handle cases for connection/disconnection of audio devices. If you want to handle connection/disconnection just on priority, there is `onAudioDeviceChanged` API 
```
// reconfigure your device
override fun onAudioDeviceChanged(freshDevices: List<MediaDevice>)
val devices = freshDevices.sortedBy { it.order }
if (devices.isNotEmpty()) {
    if (devices[0] == currentAudioDevice) return
    // choose based on priority
    audioVideo.chooseAudioDevice(devices[0])
}
```