# Amazon Chime SDK for Android
[Amazon Chime SDK Project Board](https://aws.github.io/amazon-chime-sdk-js/modules/projectboard.html)

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

We also have an [Amazon Chime SDK Project Board](https://aws.github.io/amazon-chime-sdk-js/modules/projectboard.html) where you can find community requests and their statuses.

To get started, see the following resources:

* [Amazon Chime](https://aws.amazon.com/chime)
* [Amazon Chime Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/what-is-chime.html)
* [Amazon Chime SDK API Reference](http://docs.aws.amazon.com/chime/latest/APIReference/Welcome.html)
* [SDK Documentation](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/)

And review the following guides:

* [API Overview](guides/api_overview.md)
* [Getting Started](guides/getting_started.md)
* [Frequently Asked Questions (FAQ)](#frequently-asked-questions)
* [Custom Video Sources, Processors, and Sinks](guides/custom_video.md)
* [Video Pagination with Active Speaker-Based Policy](guides/video_pagination.md)
* [Content Share](guides/content_share.md)
* [Meeting Events](guides/meeting_events.md)
* [Event Ingestion](guides/event_ingestion.md)
* [Configuring Remote Video Subscription](guides/configuring_remote_video_subscription.md)
* [Background Video Filters](guides/background_video_filters.md)

## Setup

> NOTE: If you just want to run demo application, skip to [Running the demo app](#running-the-demo-app)

The Mobile SDKs for Android could be downloaded from the Maven Central repository, by integrated into your Android project's Gradle files, or you can be directly embedded via .aar files.

For the purpose of setup, your project's root folder will be referred to as `root`.

### From Maven

To obtain the dependencies from Maven, add the dependencies to your app's (module-level) `build.gradle`.

Update `build.gradle` in `root/app` and add the following under `dependencies`:

```
dependencies {
    implementation 'software.aws.chimesdk:amazon-chime-sdk-media:$MEDIA_VERSION'
    implementation 'software.aws.chimesdk:amazon-chime-sdk:$SDK_VERSION'
}
```
The version numbers could be obtained from the latest [release](https://github.com/aws/amazon-chime-sdk-android/releases/latest).

If you don't need video and content share functionality, or software video codec support, you could use `amazon-chime-sdk-media-no-video-codecs` instead to reduce size. Exclude the usage of `amazon-chime-sdk-media` module and/or `amazon-chime-sdk-machine-learning` module from the transitive dependency of `amazon-chime-sdk`:

```
dependencies {
    implementation 'software.aws.chimesdk:amazon-chime-sdk-media-no-video-codecs:$MEDIA_VERSION'
    implementation ('software.aws.chimesdk:amazon-chime-sdk:$MEDIA_VERSION') {
        exclude module: 'amazon-chime-sdk-media'
        exclude module: 'amazon-chime-sdk-machine-learning'
    }
}
```

Projects can now build Arm and x86 targets, which may be useful if bundling an app. x86 targets will not function and are not intended to be installed or run on any x86 device or emulator.
**Important: Only Arm devices are supported.**


If you need non-functional x86 stubs in the media binary in order to bundle your app, you can append `-x86-stub` to your chosen media dependency. For example:
```
dependencies {
    implementation 'software.aws.chimesdk:amazon-chime-sdk-media-no-video-codecs-x86-stub:$MEDIA_VERSION'
    implementation ('software.aws.chimesdk:amazon-chime-sdk:$MEDIA_VERSION') {
        exclude module: 'amazon-chime-sdk-media'
        exclude module: 'amazon-chime-sdk-machine-learning'
    }
}
```
### Manually download SDK binaries
To include the SDK binaries in your own project, follow these steps.

#### 1. Download binaries

Download `amazon-chime-sdk` and `amazon-chime-sdk-media` binaries from the latest [release](https://github.com/aws/amazon-chime-sdk-android/releases/latest).

If you like to use more machine learning features, e.g. background blur/replacement, also download the `amazon-chime-sdk-machine-learning` binary from the latest [release](https://github.com/aws/amazon-chime-sdk-android/releases/latest). Otherwise, you can ignore all references to `amazon-chime-sdk-machine-learning` in the instructions below.

If you don't need video and content share functionality, or software video codec support, you could use `amazon-chime-sdk-media-no-video-codecs` instead of `amazon-chime-sdk-media` to exclude software video codecs support and reduce size. If you do, you can treat all references to `amazon-chime-sdk-media` as `amazon-chime-sdk-media-no-video-codecs` in the instructions below. 

Projects can now build Arm and x86 targets, which may be useful if bundling an app. x86 targets will not function and are not intended to be installed or run on any x86 device or emulator.
**Important: Only Arm devices are supported.**

If you need non-functional x86 stubs combined with fully functional arm architectures in order to bundle your app, you can use `amazon-chime-sdk-media-x86-stub` or `amazon-chime-sdk-media-no-video-codecs-x86-stub` media binaries and substitute them for `amazon-chime-sdk-media` references in the instructions below.

**NOTE: We do not support mixing and matching binaries from different releases.**

Unzip them and copy the aar files to `root/app/libs`

#### 2. Update gradle files

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

If you are using `amazon-chime-sdk-machine-learning` library, then add below statement as well under `dependencies`:

```
implementation(name: 'amazon-chime-sdk-machine-learning', ext: 'aar')
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

Download `amazon-chime-sdk-media` binary from the latest [release](https://github.com/aws/amazon-chime-sdk-android/releases/latest).

Download `amazon-chime-sdk-machine-learning` binary for machine learning features.

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
  - [Screen and content share](#screen-and-content-share)
  - [Metrics](#metrics)
  - [Data Message](#data-message)
  - [Stopping a session](#stopping-a-session)
  - [Amazon Voice Focus](#amazon-voice-focus)
  - [Custom Video Source](#custom-video-source)
  - [Background Blur and Replacement](#background-blur-and-replacement)

### Starting a session

#### Use case 1. Start a session.

You need to start the meeting session to start sending and receiving audio.

Start a session with default configurations:
```kotlin
meetingSession.audioVideo.start()
```

Start a session with custom configurations:

```kotlin
meetingSession.audioVideo.start(audioVideoConfiguration)
```

These configurations are available in `audioVideoConfiguration`:
- `audioMode`
- `audioDeviceCapabilities`
- `audioStreamType`
- `audioRecordingPresetOverride`
- `enableAudioRedundancy`

AudioMode: The default audio format is Stereo/48KHz i.e Stereo Audio with 48KHz sampling rate (Stereo48K). Other supported audio formats include Mono/48KHz (Mono48K) or Mono/16KHz (Mono16K). You can specify a non-default audio mode in `AudioVideoConfiguration`, and then start the meeting session.

AudioDeviceCapabilities: The default audio device capabilities are to have both the audio input and output devices enabled (`InputAndOutput`), i.e. both microphone and speaker are enabled. `InputAndOutput` will require `MODIFY_AUDIO_SETTINGS` and `RECORD_AUDIO` permissions. Other options are `OutputOnly` (microphone disabled and speaker enabled; requires `MODIFY_AUDIO_SETTINGS` permission) and `None` (both microphone and speaker disabled; does not require any audio permissions).

AudioStreamType: The default value is ```VoiceCall```. The available options are ```VoiceCall``` and ```Music```, they are equivalent of `STREAM_VOICE_CALL` and `STREAM_MUSIC` respectively in [AudioManager](https://developer.android.com/reference/android/media/AudioManager). This configuration is for addressing the audio volume [issue](https://github.com/aws/amazon-chime-sdk-android/issues/296) on Oculus Quest 2. If you don't know what it is, you probably don't need to worry about it. For more information, please refer to Android documentation: [STREAM_VOICE_CALL](https://developer.android.com/reference/android/media/AudioManager#STREAM_VOICE_CALL), [STREAM_MUSIC](https://developer.android.com/reference/android/media/AudioManager#STREAM_MUSIC).

> Note: Even though there are more available stream options in Android, currently only *STREAM_VOICE_CALL* and *STREAM_MUSIC* are supported in Amazon Chime SDK for Android.

AudioRecordingPresetOverride: The default value is ```None```. The available options are ```None```, ```Generic```, ```Camcorder```, ```VoiceRecognition``` and ```VoiceCommunication```. These are equivalent to the options
mentioned [here](https://android.googlesource.com/platform/frameworks/wilhelm/+/master/include/SLES/OpenSLES_AndroidConfiguration.h) under *Android AudioRecorder configuration*.

EnableAudioRedundancy: The default value is true. When enabled, the SDK will send redundant audio data on detecting packet loss to help reduce its effects on audio quality. More details can be found in the
*Redundant Audio* section.

#### Use case 2. Add an observer to receive audio and video session life cycle events.

> Note: To avoid missing any events, add an observer before the session starts. You can remove the observer by calling meetingSession.audioVideo.removeAudioVideoObserver(observer).

```kotlin
val observer = object : AudioVideoObserver {
    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        if (reconnecting) {
            // e.g. the network connection is dropped
        }
    }
    override fun onAudioSessionStarted(reconnecting: Boolean) {
        // Meeting session starts.
        // Can use realtime, devices APIs.
    }
    override fun onAudioSessionDropped(reconnecting: Boolean) {}
    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        // See the "Stopping a session" section for details.
    }
    override fun onAudioSessionCancelledReconnect() {}
    override fun onConnectionRecovered() {}
    override fun onConnectionBecamePoor() {}
    override fun onVideoSessionStartedConnecting() {}
    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        // Video session starts.
        // Can use video APIs.
    }
    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {}
}

meetingSession.audioVideo.addAudioVideoObserver(observer)
```

### Device

#### Use case 3. List audio devices.

List available audio devices for the meeting.

```kotlin
val audioDevices = meetingSession.audioVideo.listAudioDevices()

// A list of MediaDevice objects
audioDevices.forEach {
    logger.info(TAG, "Device type: ${it.type}, label: ${it.label}")
}
```

#### Use case 4. Choose an audio device by passing a `MediaDevice` object.

> Note: You should call chooseAudioDevice after the session started, or it'll be a no-op. You should also call chooseAudioDevice with one of the devices returned from listAudioDevices.

```kotlin
// Filter out OTHER type which is currently not supported for selection
val audioDevices = meetingSession.audioVideo.listAudioDevices().filter {
    it.type != MediaDeviceType.OTHER)
}
val device = /* An item from audioDevices */
meetingSession.audioVideo.chooseAudioDevice(device)
```

#### Use case 5. Switch cameras.

> Note: switchCamera() is a no-op if you are using a custom camera capture source. Please refer to the [Custom Video](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md#implementing-a-custom-video-source-and-transmitting) for more details.


Switch between the front or back camera on the device, if available.

```kotlin
meetingSession.audioVideo.switchCamera()
```

#### Use case 6. Add an observer to receive the updated device list.

Add a `DeviceChangeObserver` to receive a callback when a new audio device connects or when an audio device disconnects. `onAudioDeviceChanged` includes an updated device list.

```kotlin
val observer = object: DeviceChangeObserver {
    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        // A list of updated MediaDevice objects
        freshAudioDeviceList.forEach {
            logger.info(TAG, "Device type: ${it.type}, label: ${it.label}")
        }
    }
}

meetingSession.audioVideo.addDeviceChangeObserver(observer)
```

#### Use case 7. Get currently selected audio device.

> Note: `getActiveAudioDevice` API requires API level 24 or higher.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    val activeAudioDevice = meetingSession.audioVideo.getActiveAudioDevice()
}
```

For lower API levels, builders can achieve the same by tracking the selected device with the following logic:

```kotlin
var activeAudioDevice: MediaDevice? = null
override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
    val device = /* An item from freshAudioDeviceList */
    meetingSession.audioVideo.chooseAudioDevice(device)
    activeAudioDevice = device // Update current device
}
```

### Audio

#### Use case 8. Choose the audio configuration.

> When joining a meeting, each configuration will have a default if not explicitly specified when starting the audio session.
> 
> - Supported AudioMode options: *Mono/16KHz*, *Mono/48KHz*, and *Stereo/48KHz*. Default is *Stereo/48KHz*.
> - Supported AudioDeviceCapabilities options: *Input and Output*, *Output Only*, and *None*. Default is *Input and Output*.
> - Supported AudioStreamType options: *VoiceCall* and *Music*. Default is *VoiceCall*
> - Supported AudioRecordingPresetOverride options: *None*, *Generic*, *Camcorder*, *VoiceRecognition* and *VoiceCommunication*. Default is *None*.
> - Supported enableAudioRedundancy options: *true* and *false*. Default is *true*.

```kotlin
meetingSession.audioVideo.start() // starts the audio video session with defaults mentioned above

meetingSession.audioVideo.start(audioVideoConfiguration) // starts the audio video session with the specified [AudioVideoConfiguration]
```

> Note: So far, you've added observers to receive device and session lifecycle events. In the following use cases, you'll use the real-time API methods to send and receive volume indicators and control mute state.

#### Use case 9. Mute and unmute an audio input.

```kotlin
val muted = meetingSession.audioVideo.realtimeLocalMute() // returns true if muted, false if failed

val unmuted = meetingSession.audioVideo.realtimeLocalUnmute() // returns true if unmuted, false if failed
```

#### Use case 10. Add an observer to receive realtime events such as volume changes/signal change/muted status attendees.

You can use this to build real-time indicators UI and get them updated for changes delivered by the array.


> Note: These callbacks will only include the delta from the previous callback.

```kotlin
val observer = object : RealtimeObserver {
    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        volumeUpdates.forEach { (attendeeInfo, volumeLevel) ->
            logger.info(TAG, "${attendeeInfo.attendeeId}'s volume changed: " +
                $volumeLevel // Muted, NotSpeaking, Low, Medium, High
            )
        }
    }

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        signalUpdates.forEach { (attendeeInfo, signalStrength) ->
            logger.info(TAG, "${attendeeInfo.attendeeId}'s signal strength changed: " +
                $signalStrength // None, Low, High
            )
        }
    }

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { logger.info(TAG, "${attendeeInfo.attendeeId} joined the meeting") }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { logger.info(TAG, "${attendeeInfo.attendeeId} left the meeting") }
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { logger.info(TAG, "${attendeeInfo.attendeeId} dropped from the meeting") }
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { logger.info(TAG, "${attendeeInfo.attendeeId} muted") }
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { logger.info(TAG, "${attendeeInfo.attendeeId} unmuted") }
    }
}

meetingSession.audioVideo.addRealtimeObserver(observer)
```

#### Use case 11. Detect active speakers and active scores of speakers.

You can use the `onActiveSpeakerDetected` event to enlarge or emphasize the most active speaker’s video tile if available. By setting the `scoreCallbackIntervalMs` and implementing `onActiveSpeakerScoreChanged`, you can receive scores of the active speakers periodically.

```kotlin
val observer = object : ActiveSpeakerObserver {
    override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
        if (attendeeInfo.isNotEmpty()) {
            logger.info(TAG, "${attendeeInfo[0].attendeeId} is the most active speaker")
        }
    }

    // Set to receive onActiveSpeakerScoreChanged event at interval of 1s
    override val scoreCallbackIntervalMs: Int? get() = 1000

    override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
        val scoreString = scores.map { entry -> "${entry.key.attendeeId}: ${entry.value}" }.joinToString(",")
        logger.info(TAG, "Scores of active speakers are: $scoreString")
    }
}

// Calculating the active speaker base on the SDK provided policy, you can provide any custom algorithm
meetingSession.audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), observer)
```

### Video

> Note: You'll need to bind the video to a `VideoRenderView` to render it.
>
> A local video tile can be identified using the `isLocalTile` property.
>
> A content video tile can be identified using the `isContent` property. See [Screen and content share](#screen-and-content-share).
>
> A tile is created with a new tile ID when the same remote attendee restarts the video.


You can find more details on adding/removing/viewing video from [Building a meeting application on android using the Amazon Chime SDK](https://aws.amazon.com/blogs/business-productivity/building-a-meeting-application-on-android-using-the-amazon-chime-sdk/).

#### Use case 12. Start receiving remote videos.

You can call `startRemoteVideo` to start receiving remote videos, as this doesn’t happen by default.

```kotlin
meetingSession.audioVideo.startRemoteVideo()
```

#### Use case 13. Stop receiving remote videos.

`stopRemoteVideo` stops receiving remote videos and triggers `onVideoTileRemoved` for existing remote videos.

```kotlin
meetingSession.audioVideo.stopRemoteVideo()
```

#### Use case 14. View remote videos.

```kotlin
val observer = object : VideoTileObserver {
    override fun onVideoTileAdded(tileState: VideoTileState) {
        // Ignore local video (see View local video), content video (seeScreen and content share)
        if(tileState.isLocalTile || tileState.isContent) return

        val videoRenderView = /* a VideoRenderView object in your application to show the video */
        meetingSession.audioVideo.bindVideoView(videoRenderView, tileState.tileId)
    }

    override onVideoTileRemoved(tileState: VideoTileState) {
        // unbind video view to stop viewing the tile
        audioVideo.unbindVideoView(tileState.tileId)
    }
}

meetingSession.audioVideo.addVideoTileObserver(observer)
```

For more advanced video tile management, take a look at [Video Pagination](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/video_pagination.md).

#### Use case 15. Start sharing your video.

```kotlin
// Use internal camera capture for the local video
meetingSession.audioVideo.startLocalVideo()

// Use internal camera capture and set configuration for the video, e.g. maxBitRateKbps
// If maxBitRateKbps is not set, it will be self adjusted depending on number of users and videos in the meeting
// This can be called multiple times to dynamically adjust video configuration
val localVideoConfig = LocalVideoConfiguration(600)
meetingSession.audioVideo.startLocalVideo(localVideoConfig)

// You can switch camera to change the video input device
meetingSession.audioVideo.switchCamera()

// Or you can inject custom video source for local video, see custom video guide
```

#### Use case 16. Stop sharing your video.

```kotlin
meetingSession.audioVideo.stopLocalVideo()
```

#### Use case 17. View local video.

```kotlin
val observer = object : VideoTileObserver {
    override fun onVideoTileAdded(tileState: VideoTileState) {
        // onVideoTileAdded is called after startLocalVideo
        val localVideoRenderView = /* a VideoRenderView object to show local video */

        if (tileState.isLocalTile) {
            audioVideo.bindVideoView(localVideoRenderView, tileState.tileId)
        }
    }

    override onVideoTileRemoved(tileState: VideoTileState) {
        // onVideoTileRemoved is called after stopLocalVideo
        if (tileState.isLocalTile) {
            logger.info(TAG, "Local video is removed")
            audioVideo.unbindVideoView(tileState.tileId)
        }
    }
}

meetingSession.audioVideo.addVideoTileObserver(observer)
```

### Screen and content share

> Note: When you or other attendees share content (e.g., screen capture or any other VideoSource object), the content attendee (attendee-id#content) joins the session and shares content as if a regular attendee shares a video.
>
> For example, your attendee ID is "my-id". When you call `meetingSession.audioVideo.startContentShare`, the content attendee "my-id#content" will join the session and share your content.

#### Use case 18. Start sharing your screen or content.

```kotlin
val observer = object : ContentShareObserver {
    override fun onContentShareStarted() {
        logger.info(TAG, "Content share started")
    }

    override fun onContentShareStopped(status: ContentShareStatus) {
        logger.info(TAG, "Content share stopped with status ${status.statusCode}")
    }
}

meetingSession.audioVideo.addContentShareObserver(observer)
val contentShareSource = /* a ContentShareSource object, can use DefaultScreenCaptureSource for screen share or any subclass with custom video source */
// ContentShareSource object is not managed by SDK, builders need to start, stop, release accordingly
meetingSession.audioVideo.startContentShare(contentShareSource)
```

You can set configuration for content share, e.g. maxBitRateKbps. Actual quality achieved may vary throughout the call depending on what system and network can provide.
```kotlin
val contentShareConfig = LocalVideoConfiguration(200)
meetingSession.audioVideo.startContentShare(contentShareSource, contentShareConfig)
```

See [Content Share](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/content_share.md) for more details.

#### Use case 19. Stop sharing your screen or content.

```kotlin
meetingSession.audioVideo.stopContentShare()
```

#### Use case 20. View attendee content or screens.

Chime SDK allows two simultaneous content shares per meeting. Remote content shares will trigger `onVideoTileAdded`, while local share will not. To render the video for preview, add a `VideoSink` to the `VideoSource` in the `ContentShareSource`.

```kotlin
val observer = object : VideoTileObserver {
    override fun onVideoTileAdded(tileState: VideoTileState) {
        if (tileState.isContent) {
            // tileState.attendeeId is formatted as "attendee-id#content"
            val attendeeId = tileState.attendeeId
            // Get the attendee ID from "attendee-id#content"
            val baseAttendeeId = DefaultModality(attendeeId).base()
            logger.info(TAG, "$baseAttendeeId is sharing screen")

            val contentVideoRenderView = /* a VideoRenderView object in your application to show the content video */
            meetingSession.audioVideo.bindVideoView(contentVideoRenderView, tileState.tileId)
        }
    }

    override onVideoTileRemoved(tileState: VideoTileState) {
        // unbind video view to stop viewing the tile
        meetingSession.audioVideo.unbindVideoView(tileId)
    }
}

meetingSession.audioVideo.addVideoTileObserver(observer)
```

### Metrics

#### Use case 21. Add an observer to receive the meeting metrics.

See `ObservableMetric` for more available metrics and to monitor audio, video, and content share quality.

```kotlin
val observer = object: MetricsObserver {
    override fun onMetricsReceived(metrics: Map<ObservableMetric, Any>) {
        metrics.forEach { (metricsName, metricsValue) ->
            logger.info(TAG, "$metricsName : $metricsValue")
        }
    }
}

meetingSession.audioVideo.addMetricsObserver(observer)
```

### Data Message

#### Use case 22. Add  an observer to receive data message.

You can receive real-time messages from multiple topics after starting the meeting session.

> Note: Data messages sent from local participant will not trigger this callback unless it's throttled.

```kotlin
val YOUR_ATTENDEE_ID = meetingSession.configuration.credentials.attendeeId

val observer = object: DataMessageObserver {
    override fun onDataMessageReceived(dataMessage: DataMessage) {
        // A throttled message is returned by backend
        if (!dataMessage.throttled) {
            logger.info(TAG, "[${dataMessage.timestampMs}][{$dataMessage.senderAttendeeId}] : ${dataMessage.text()}")
    }
}

// You can subscribe to multiple topics.
const val DATA_MESSAGE_TOPIC = "chat"
meetingSession.audioVideo.addRealtimeDataMessageObserver(DATA_MESSAGE_TOPIC, observer)
```

#### Use case 23. Send data message.

You can send real time message to any topic, to which the observers that have subscribed will be notified.

> Note: Topic needs to be alpha-numeric and it can include hyphen and underscores. Data cannot exceed 2kb and lifetime is optional but positive integer.

```kotlin
const val DATA_MESSAGE_TOPIC = "chat"
const val DATA_MESSAGE_LIFETIME_MS = 1000

// Send "Hello Chime" to any subscribers who are listening to "chat" topic with 1 seconds of lifetime
meetingSession.audioVideo.realtimeSendDataMessage(
    DATA_MESSAGE_TOPIC,
    "Hello Chime",
    DATA_MESSAGE_LIFETIME_MS
)
```

### Stopping a session

> Note: Make sure to remove all the observers and release resources you have added to avoid any memory leaks.

#### Use case 24. Stop a session.

```kotlin
val observer = object: AudioVideoObserver {  
    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        // This is where meeting ended.
        // You can do some clean up work here.
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        // This will be invoked as well.
    }
}

meetingSession.audioVideo.addAudioVideoObserver(observer)
meetingSession.audioVideo.stop()
```

### Amazon Voice Focus

Amazon Voice Focus reduces the background noise in the meeting for better meeting experience. For more details, see [Amazon Voice Focus](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md#11-using-amazon-voice-focus-optional).

#### Use case 25. Enable/Disable Amazon Voice Focus.

```kotlin
val enbabled = meetingSession.audioVideo.realtimeSetVoiceFocusEnabled(true) // enabling Amazon Voice Focus successful

val disabled = meetingSession.audioVideo.realtimeSetVoiceFocusEnabled(false) // disabling Amazon Voice Focus successful
```

### Custom Video Source

Custom video source allows you to control the video, such as applying a video filter. For more details, see [Custom Video](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md).

### Background Blur and Replacement

Background Blur/Replacement allows you to apply blur on or replace background of your video with an image. For more details, see [BackgroundFilter](guides/background_video_filters.md).

### Redundant audio

Starting from version 0.18.3, the SDK starts sending redundant audio data to our servers on detecting packet loss
to help reduce its effect on audio quality. Redundant audio packets are only sent out for packets containing active
audio, i.e. speech or music. This may increase the bandwidth consumed by audio to up to 3 times the normal amount
depending on the amount of packet loss detected. The SDK will automatically stop sending redundant data if it hasn't
detected any packet loss for 5 minutes.

If you need to disable this feature, you can do so through the AudioVideoConfiguration before starting the session.

```kotlin
meetingSession.audioVideo.start(AudioVideoConfiguration(enableAudioRedundancy = false))
```

While there is an option to disable the feature, we recommend keeping it enabled for improved audio quality.
One possible reason to disable it might be if your customers have very strict bandwidth limitations.

## Frequently Asked Questions

Refer to [General FAQ](https://aws.github.io/amazon-chime-sdk-js/modules/faqs.html) for Amazon Chime SDK.

### Debugging

#### How can I get Amazon Chime SDK logs for debugging?
Applications can get logs from Chime SDK by passing instances of Logger when creating [MeetingSession](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session/index.html). Amazon Chime SDK has some default implementations of logger that your application can use, such as [ConsoleLogger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-console-logger/index.html) which logs into console. `ConsoleLogger` is set to `INFO` level as default. Therefore, in order to get all logs, including media logs, create logger by following:
```kotlin
val logger = ConsoleLogger(LogLevel.VERBOSE)
```

#### Remote attendees cannot hear my audio, what do I do?
The SDK uses [OpenSL ES](https://developer.android.com/ndk/guides/audio/opensl/opensl-for-android) underneath which requires the setting of recording presets while opening the connection to the microphone device. We have discovered that there isn't a specific preset value that works well on all possible android devices. The SDK uses a default preset of `VoiceCommunication` which we have arrived at after running some tests on the devices in our possession. If this default preset does not work and is leading to the remote party not being able to hear you, please try starting the session with a different recording preset by specifying `audioRecordingPresetOverride` in the `AudioVideoConfiguration` that is passed into the start API.
```kotlin
// Creating a config where the preset is overriden with Generic (for example)
val audioVideoConfig = AudioVideoConfiguration(audioRecordingPresetOverride = AudioRecordingPresetOverride.Generic)
// Start Audio Video
audioVideo.start(audioVideoConfig)
```

## Notice

The use of background replacement is subject to additional notice. You and your end users are responsible for all Content (including any images) uploaded for use with background replacement, and must ensure that such Content does not violate the law, infringe or misappropriate the rights of any third party, or otherwise violate a material term of your agreement with Amazon (including the documentation, the AWS Service Terms, or the Acceptable Use Policy).

---

Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
