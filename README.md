# Amazon Chime SDK for Android

> Note: If building with the SDK source code, the `development` branch contains bleeding-edge changes that may not build with the publically available Chime media library or may not be as stable as [public releases](https://github.com/aws/amazon-chime-sdk-android/releases).

## Build video calling, audio calling, and screen sharing applications powered by Amazon Chime.

The Amazon Chime SDK for Android makes it easy to add collaborative audio calling,
video calling, and screen share viewing features to Android applications by 
using the same infrastructure services that power meetings on the Amazon 
Chime service.

This Amazon Chime SDK for Android works by connecting to meeting session
resources that you have created in your AWS account. The SDK has everything
you need to build custom calling and collaboration experiences in your
Android application, including methods to: configure meeting sessions, list 
and select audio devices, switch video devices, start and stop screen share 
viewing, receive callbacks when media events occur such as volume changes, 
and manage meeting features such as audio mute and video tile bindings.

To get started, see the following resources:

* [Amazon Chime](https://aws.amazon.com/chime)
* [Amazon Chime Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/what-is-chime.html)
* [Amazon Chime SDK API Reference](http://docs.aws.amazon.com/chime/latest/APIReference/Welcome.html)
* [SDK Documentation](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/)

And review the following guides:

* [API Overview](guides/api_overview.md)
* [Getting Started](guides/getting_started.md)
* [Custom Video Sources, Processors, and Sinks](guides/custom_video.md)
* [Video Pagination with Active Speaker-Based Policy](guides/video_pagination.md)

## Setup

> NOTE: If you just want to run demo application, skip to [Running the demo app](#running-the-demo-app)

To include the SDK binaries in your own project, follow these steps.

For the purpose of setup, your project's root folder will be referred to as `root`

### 1. Download binaries

Download the following zips:

* [amazon-chime-sdk-0.9.1.tar.gz](https://amazon-chime-sdk-android.s3.amazonaws.com/sdk/0.9.1/amazon-chime-sdk-0.9.1.tar.gz)
* [amazon-chime-sdk-media-0.9.0.tar.gz](https://amazon-chime-sdk-android.s3.amazonaws.com/media/0.9.0/amazon-chime-sdk-media-0.9.0.tar.gz)

Unzip them and copy the aar files to `root/app/libs`

### 2. Update gradle files

Update `build.gradle` in `root` by adding the following under `repositories` in `allprojects`:

```
allprojects {
   repositories {
      jcenter()
      flatDir {
        dirs 'libs'
      }
   }
}
```

Update `build.gradle` in `root/app` and add the following under `dependencies`:

```
implementation(name: 'amazon-chime-sdk', ext: 'aar')
implementation(name: 'amazon-chime-sdk-media', ext: 'aar')
```

Update `build.gradle` in `root/app` under `compileOptions`:

```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

## Running the demo app

> NOTE: This is just to run demo application and use SDK as code instead of aar library. 

To run the demo application, follow these steps.

> NOTE: Please make sure that you are running on ARM supported devices (real devices) or simulator with arm supported. We do not support x86 currently, so simulators with x86 will not work.

### 1. Deploy serverless demo

Deploy the serverless demo from [amazon-chime-sdk-js](https://github.com/aws/amazon-chime-sdk-js), which returns `https://xxxxx.xxxxx.xxx.com/Prod/`

Provide `https://xxxxx.xxxxx.xxx.com/Prod/` for mobile demo app.

### 2. Download binary

Download the following zip:

* [amazon-chime-sdk-media-0.9.0.tar.gz](https://amazon-chime-sdk-android.s3.amazonaws.com/media/0.9.0/amazon-chime-sdk-media-0.9.0.tar.gz)

Unzip and copy the aar files to `amazon-chime-sdk-android/amazon-chime-sdk/libs`

### 3. Update demo app

Update `test_url` in `strings.xml` at the path `amazon-chime-sdk-android/app/src/main/res/values` 
with the URL of the serverless demo deployed in Step 1.

> NOTE: use `https://xxxxx.xxxxx.xxx.com/Prod/`

## Reporting a suspected vulnerability

If you discover a potential security issue in this project we ask that you notify AWS/Amazon Security via our
[vulnerability reporting page](http://aws.amazon.com/security/vulnerability-reporting/). Please do **not** create a public GitHub issue.

## Usage
  - [Starting a session](#starting-a-session)
  - [Device](#device)
  - [Audio](#audio)
  - [Video](#video)
  - [Screen share](#screen-share)
  - [Metrics](#metrics)
  - [Data Message](#data-message)
  - [Stopping a session](#stopping-a-session)
  - [Voice Focus](#voice-focus)
  - [Custom Video Source](#custom-video-source)

### Starting a session

#### Use case 1. Start a session. 

To start sending/receiving audio, you’ll just need to start the session.

```kotlin
audioVideo.start()
```

#### Use case 2. Add an observer to receive audio and video session life cycle events. 

```kotlin
class MainActivity: AudioVideoObserver {
    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {}
    override fun onAudioSessionStarted(reconnecting: Boolean) {
        // It is recommeded to handle device selection such as listAudioDevice and chooseAudioDevice
        // and handle mute of self attendee.
        // This is due to [choose-audio-device](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/choose-audio-device.html)/mute/unmute needs the session to be started
    }
    override fun onAudioSessionDropped(reconnecting: Boolean) {}
    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        // You can handle some clean up logics
        // One example might be showing user the meeting ended
    }
    override fun onAudioSessionCancelledReconnect() {}
    override fun onConnectionRecovered() {}
    override fun onConnectionBecamePoor() {}
    override fun onVideoSessionStartedConnecting() {}
    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        // Handle logic of receiving video/starting local video
    }
    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {}

    audioVideo.addAudioVideoObserver(this)
}
```

### Device

#### Use case 3. List audio devices

List available devices that can be used for the meeting.

```kotlin
// You will receieve something similar to [Pixel 3 XL (Handset), Pixel 3 XL (Speaker), Galaxy Buds+ (F4C1) (Bluetooth)]
val devices = audioVideo.listAudioDevices()
```

#### Use case 4. Choose audio device by passing `MediaDevice` object

> NOTE: chooseAudioDevice is no-op if it is called before audioVideo.start(). You should call this after session started. 



> NOTE: You should call chooseAudioDevice with one of devices returned from listAudioDevices()

```kotlin
val devices = audioVideo.listAudioDevices().filter {
    it.type != MediaDeviceType.OTHER)
}.sortedBy { it.order } // get devices by its priority. You can use your own logic
// For instance, current order is in bluetooth -> earphone -> speaker -> built-in earpiece
if (devices.isNotEmpty()) {
    audioVideo.chooseAudioDevice(devices[0]) 
}            
```

#### Use case 5. Switch between camera

> NOTE: switchCamera() is no-op if you are using custom camera capture source. Please refer [custom_video](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md#implementing-a-custom-video-source-and-transmitting) for more details.


`switchCamera` will switch currently active camera. In order to get active camera, you can call [get-active-camera](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/get-active-camera.html)

```kotlin
audioVideo.switchCamera() // front will switch to back and back will switch to front camera
```

#### Use case 6. Subscribe to get updated device list

Add `DeviceChangeObserver` to receive callback when new audio device is connected or audio device has been disconnected. For instance, if a bluetooth audio device is connected, `onAudioDeviceChanged` is called with the device list including the headphone.

```kotlin
class MainActivity: DeviceChangeObserver {
    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        // handle audio device selection or use your custom logic
        // You will receieve something similar to [Pixel 3 XL (Handset), Pixel 3 XL (Speaker), Galaxy Buds+ (F4C1) (Bluetooth)]
        val devices = freshAudioDeviceList.filter{ it.type != MediaDeviceType.OTHER}.sortedBy{ it.order }
        if (devices.isNotEmpty()) {
            audioVideo.chooseAudioDevice(device[0])
        }
    }
     
    audioVideo.addDeviceChangeObserver(this) 
}



```

#### Use case 7. Get currently selected audio device

> NOTE: `getActiveAudioDevice` API is supported from API Android 24 >= (Android 7.0) or higher.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    val activeAudioDevice = audioVideo.getActiveAudioDevice()
}
```

For versions, lower than Android 7.0, builders can achieve the same by manually implementing the following logic.
Whenever you call `audioVideo.chooseAudioDevice` , you’ll have to manually update your `currentAudioDevice`.

```kotlin
var activeAudioDevice: MediaDevice? = null
override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
    val device = // an element in the list of current available audio devices
    if (devices.isNotEmpty()) {
        audioVideo.chooseAudioDevice(device)
        activeAudioDevice = device // Update current device 
    }
}
```

### Audio

#### Use case 8. Mute and unmute an audio input

```kotlin
val muted = audioVideo.realtimeLocalMute() // returns true if muted, false if failed

val unmuted = audioVideo.realtimeLocalUnmute() // returns true if unmuted, false if failed
```

#### Use case 9. Add an observer to observe realtime events such as volume changes/signal change/muted status attendees. 

You can use this to build real-time indicator UI on specific attendee

```kotlin
class MainActivity: RealtimeObserver {
    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        // Update UI to show some volume level of attendees
    }
    
    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        // Update UI to show some signal stregth (network condition) of attendees
    }
    
    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        // Update UI to show attendees
    }
    
    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        // Update UI to remove attendees
    }
    
    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        // Update UI to remove attendees
    }
    
    override fun fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) }
        // Update UI to show muted status of attendees
    }
    
    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        // Update UI to show unmuted status of attendees
    }
    
    audioVideo.addRealtimeObserver(this)
}
```

#### Use case 10. Detect active speakers. 

> NOTE: You need to set `scoreCallbackIntervalMs` to receive callback for `onActiveSpeakerScoreChanged`. If this value is not set, you will only get `onActiveSpeakerDetected` callback. For basic use case, you can just use `onActiveSpeakerDetected.`

```kotlin
class MainActivity: ActiveSpeakerObserver {
    override val scoreCallbackIntervalMs: Int? get() = 1000
    
    override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
        // Just handle logic of active speakers. 
        // Most use case should be handled by this callack
        // This will be sorted from most active to least active
    }
    
    override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
       // handle logic based on active speaker score changed.
       // You can compare them to get most active speaker
    }
    
    // Use default policy for active speaker. 
    // If you want custom logic, implement your own ActiveSpeakerPolicy
    audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), this)
}
```

### Video

> NOTE: You will need to bind the video to `DefaultVideoRenderView` or your customer render view (`VideoRenderView`) in order to display the video. There are also few things to note.

You can find more details on adding/removing/viewing video from [building-a-meeting-application-on-android-using-the-amazon-chime-sdk/](https://aws.amazon.com/blogs/business-productivity/building-a-meeting-application-on-android-using-the-amazon-chime-sdk/)

#### Use case 11. Start receiving remote videos.

```kotlin
audioVideo.startRemoteVideo()
```

#### Use case 12. Start viewing remote video tiles.

```kotlin
class MainActivity: VideoTileObserver {
    override fun onVideoTileAdded(tileState: VideoTileState) {
       if (!tileState.isLocalTile) {
            // In order to use binding, you can read more about
            // https://developer.android.com/topic/libraries/data-binding
            
            // binding.remoteVideoView is DefaultRenderView defined in the layout
            // You can also use val remoteVideoView = findViewById(...)
            audioVideo.bindVideoView(binding.remoteVideoView, tileState.tileId)
       }
    }
    
    audioVideo.addVideoTileObserver(this)
}
```

#### Use case 13. Stop receiving remote videos.

```kotlin
audioVideo.stopRemoteVideo()
```

#### Use case 14. Stop viewing remote videos.

```kotlin
class MainActivity: VideoTileObserver {
    override onVideoTileRemoved(tileState: VideoTileState) {
        // unbind video view to stop viewing the tile
        audioVideo.unbindVideoView(tileId)
    }
    
    audioVideo.addVideoTileObserver(this)
}
```

#### Use case 15. Start sharing your video. 

> NOTE: From `onVideoTileAdded` callback, tileState should have property of `isLocalTile` true

```kotlin
// start sharing local video
// startLocalVideo will invoke onVideoTileAdded callback
 audioVideo.startLocalVideo()
```

#### Use case 16. Start viewing local video tile.

```kotlin
class MainActivity : VideoTileObserver {
    // NOTE: Details on creating adapter is omitted.
    override fun onVideoTileAdded(tileState: VideoTileState) {
        if (tileState.isLocalTile) {
            // In order to use binding, you can read more about
            // https://developer.android.com/topic/libraries/data-binding
            
            // binding.remoteVideoView is DefaultRenderView defined in the layout
            // You can also use val localVideoView = findViewById(...) and pass it
            audioVideo.bindVideoView(binding.localVideoView, tileState.tileId)
        }
    }
    
    audioVideo.addVideoTileObserver(this)
}
```

#### Use case 17. Stop sharing your video.

```kotlin
// stop sharing local video
// stopLocalVideo will invoke onVideoTileRemoved callback
audioVideo.stopLocalVideo()
```

#### Use case 18. Stop viewing local video tile.

```kotlin
class MainActivity : VideoTileObserver {
    override onVideoTileRemoved(tileState: VideoTileState) {
        // unbind video view to stop viewing the tile
        audioVideo.unbindVideoView(tileId)
    }
    
    audioVideo.addVideoTileObserver(this)
}
```

If you want more advanced video management, take a look at [video_pagination](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/video_pagination.md)

### Screen share

#### Use case 19. Start viewing remote screen share.

```kotlin
class MainActivity : VideoTileObserver {
    // NOTE: Details on creating adapter is omitted.
    
    override fun onVideoTileAdded(tileState: VideoTileState) {
        if (tileState.isContent) {
            // In order to use binding, you can read more about
            // https://developer.android.com/topic/libraries/data-binding
            
            // binding.remoteVideoView is DefaultRenderView defined in the layout
            // You can also use val localVideoView = findViewById(...) and pass it
            audioVideo.bindVideoView(binding.screenVideoView, tileState.tileId)
        }
    }
    
    override onVideoTileRemoved(tileState: VideoTileState) {
        audioVideo.unbindVideoView(tileId)
    }
    
    audioVideo.addVideoTileObserver(this)
}
```

### Metrics

#### Use case 20. Start receiving metrics

```kotlin
class MainActivity: MetricsObserver {
    override fun onMetricsReceived(metrics: Map<ObservableMetric, Any>) {
        // log any metrics
    }
    
    audioVideo.addMetricsObserver(this) 
}
```

### Data Message

#### Use case 21. Start receiving data message

You can receive real-time message from subscribed topic. 

> NOTE: topic needs to be alpha-numeric and it can include hyphen and underscores.

```kotlin
const val DATA_MESSAGE_TOPIC = "chat"
class MainActivity : DataMessageObserver {
    override fun onDataMessageReceived(dataMessage: DataMessage) {
        // handle data message
    }
    
    // You can also subscribe to different topic
    audioVideo.addRealtimeDataMessageObserver(DATA_MESSAGE_TOPIC, this)
}
```

#### Use case 22. Start sending data message

You can send real time message to any subscribed topic. 

> NOTE: topic needs to be alpha-numeric and it can include hyphen and underscores. Data cannot exceed 2kb and lifetime should be positive integer 

```kotlin
const val DATA_MESSAGE_TOPIC = "chat"
const val DATA_MESSAGE_LIFETIME_MS = 1000

// Send "Hello Chime" to any subscribers who are listening to "chat" topic with 1 seconds of lifetime
audioVideo.realtimeSendDataMessage(
    DATA_MESSAGE_TOPIC,
    "Hello Chime",
    DATA_MESSAGE_LIFETIME_MS
)
```

### Stopping a session

> NOTE: Make sure to remove all the observers you have added to avoid any memory leaks.

#### Use case 23. Stop a session

```kotlin
class MainActivity : AudioVideoObserver, ... {
   
    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        // This is where meeting ended.
        // You can do some clean up work here.
    }
    
    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        // This will be invoked   
    }
    
    override fun onDestroy() {
        // Remove all the observers if you have add them. 
        // You do not have to remove them all if you haven't added.
        audioVideo.removeAudioVideoObserver(this)
        audioVideo.removeDeviceChangeObserver(this)
        audioVideo.removeMetricsObserver(this)
        audioVideo.removeRealtimeObserver(this)
        audioVideo.removeRealtimeDataMessageObserverFromTopic(DATA_MESSAGE_TOPIC)
        audioVideo.removeVideoTileObserver(this)
        audioVideo.removeActiveSpeakerObserver(this)
    }
    
    audioVideo.addAudioVideoObserver(this)
    audioVideo.stop()
}
```

### Voice Focus

Voice focus reduces the background noise in the meeting for better meeting experience. For more details, see [api_overview.md#11-using-amazon-voice-focus-optional](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md#11-using-amazon-voice-focus-optional)

#### Use case 24. Enable/Disable voice focus

```kotlin
val success = audioVideo.realtimeSetVoiceFocusEnabled(true) // success = enabling voice focus successful

val success = audioVideo.realtimeSetVoiceFocusEnabled(false) // success = enabling voice focus successful
```

### Custom Video Source

Custom video source allows you to inject your own source to control the video such as applying filter to the video. Detailed guides can be found in [custom_video.md](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md)


---

Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
