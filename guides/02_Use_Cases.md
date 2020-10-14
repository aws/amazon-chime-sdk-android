# Use Cases

This guides provides use cases of Chime Mobile SDK

## Display currently used audio device from list
For Android API 24 >=, You can use `getActiveAudioDevice` in order to achieve display of currently active audio device. For instance, if you see `DeviceAdapter` in our demo app
```kt
if (Build.VERSION.SDK_INT >= AUDIO_RECORDING_CONFIG_API_LEVEL) {
    view.text = if (currentDevice == devices[position]) "${devices[position]} ✓" else devices[position].toString()
}
```
You can use `getActiveAudioDevice` to get currentDevice and put a check mark or make some UI change

In order to support from Android API 21 - 23 (5.0 - 6.0), we need to manually handle cases for connection/reconnection. Since there is optional API `onChooseAudioDeviceCalled` which is called when chooseAudioDevice is called. This will let you make less mistakes. 
```
    override fun onChooseAudioDeviceCalled(device: MediaDevice) {
        audioDeviceManager.setCurrentAudioDevice(device) // update your current device
    }
```

However, you'll still need to handle cases for connection/disconnection of audio devices. If you want to handle connection/disconnection just on priority, there is `onAudioDeviceChanged` API 
```
// reconfigure your device
override fun onAudioDeviceChanged(freshDevices: List<MediaDevice>) {
val devices = freshDevices.sortedBy { it.order }
if (devices.isNotEmpty()) {
    if (devices[0] == currentAudioDevice) return
    // choose based on priority
    audioVideo.chooseAudioDevice(devices[0])
}
```