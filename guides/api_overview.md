# API Overview

This guide gives an overview of the API methods that you can use to create a meeting with audio and 
video.

## 1. Create a session

The [MeetingSession](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session/index.html) and its [AudioVideoFacade](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-facade/index.html) are the starting points for creating meetings. 
You will need to create a [Logger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-logger/index.html) and [MeetingSessionConfiguration](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-configuration/index.html) before creating a meeting
session.

### 1a. Create a logger

You can utilize the [ConsoleLogger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-console-logger/index.html) to write logs with Android's [Log](https://developer.android.com/reference/android/util/Log). You can also implement 
the Logger interface to customize the logging behavior.

```
val logger = ConsoleLogger(LogLevel.DEBUG) 
```

### 1b. Create a meeting session configuration

Create a [MeetingSessionConfiguration](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-configuration/index.html) object with the responses to [chime:CreateMeeting](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeeting.html) and 
[chime:CreateAttendee](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html). Your server application should make these API calls and securely pass the 
meeting and attendee responses to the client application.

### 1c. Create a meeting session

Using the above objects and an application context, create a [DefaultMeetingSession](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-default-meeting-session/index.html).

```
val meetingSession = DefaultMeetingSession(sessionConfig, logger, applicationContext)
```

## 2. Configure the session

Before starting the meeting session, you should configure the audio device.

### 2a. Configure the audio device

To retrieve a list of available audio devices, call meetingSession.audioVideo.[listAudioDevices()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/list-audio-devices.html).

To use the chosen audio device, call meetingSession.audioVideo.[chooseAudioDevice(mediaDevice)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/choose-audio-device.html).

### 2b. Register a device change observer (optional)

You can receive events about changes to available audio devices by implementing a 
[DeviceChangeObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-change-observer/index.html) and registering the observer with the audio video facade.

To add a DeviceChangeObserver, call meetingSession.audioVideo.[addDeviceChangeObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/add-device-change-observer.html).

To remove a DeviceChangeObserver, call meetingSession.audioVideo.[removeDeviceChangeObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/remove-device-change-observer.html).

A DeviceChangeObserver has the following method:

* [onAudioDeviceChanged](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-change-observer/on-audio-device-changed.html): called when audio devices are changed

## 3. Request permissions for audio and video

Before starting audio or video, you will need to request permissions from the user and verify that
they are granted. Otherwise, the API will throw a `SecurityException`. Amazon Chime SDK for Android 
already declares the following permissions in its manifest file.

Video permissions:
```
Manifest.permission.CAMERA
```

Based on which `AudioDeviceCapabilities` you plan to have users join meetings with, you will need to
request certain audio permissions and declare them in your manifest file.

Audio permissions:
```
Manifest.permission.MODIFY_AUDIO_SETTINGS // Required for `AudioDeviceCapabilities.InputAndOutput` and `AudioDeviceCapabilities.OutputOnly`
Manifest.permission.RECORD_AUDIO          // Required for `AudioDeviceCapabilities.InputAndOutput`
```

## 4. Register an audio video observer 

You can receive events about the audio session, video session, and connection health by implementing
an [AudioVideoObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/index.html) and registering the observer with the audio video facade.

To add an AudioVideoObserver, call meetingSession.audioVideo.[addAudioVideoObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/add-audio-video-observer.html).

To remove an AudioVideoObserver, call meetingSession.audioVideo.[removeAudioVideoObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/remove-audio-video-observer.html).

An AudioVideoObserver has the following methods:

* [onAudioSessionStartedConnecting](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-audio-session-started-connecting.html): called when the audio session is connecting or reconnecting
* [onAudioSessionStarted](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-audio-session-started.html): called when the audio session has started
* [onAudioSessionDropped](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-audio-session-dropped.html): called when the audio session got dropped due to poor network conditions
* [onAudioSessionStopped](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-audio-session-stopped.html): called when the audio session has stopped
* [onAudioSessionCancelledReconnect](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-audio-session-cancelled-reconnect.html): called when the audio session cancelled reconnecting
* [onConnectionBecamePoor](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-connection-became-poor.html) : called when connection health has become poor
* [onConnectionRecovered](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-connection-recovered.html): called when connection health has recovered
* [onVideoSessionStartedConnecting](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-video-session-started-connecting.html): called when the video session is connecting or reconnecting
* [onVideoSessionStarted](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-video-session-started.html): called when the video session has started
* [onVideoSessionStopped](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-video-session-stopped.html): called when the video session has stopped

## 5. Starting and stopping the meeting session

Call this method after doing pre-requisite configuration (See previous sections).

To start the meeting session, call meetingSession.audioVideo.[start()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start.html). This will start underlying
media clients and will start sending and receiving audio.

To stop the meeting session, call meetingSession.audioVideo.[stop()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/stop.html). 

## 6. Building a roster of participants

### 6a. Register a realtime observer 

You can use a [RealtimeObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/index.html) to learn when attendees join and leave and when their volume 
level, mute state, or signal strength changes.

To add a RealtimeObserver, call meetingSession.audioVideo.[addRealtimeObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/add-realtime-observer.html).

To remove a RealtimeObserver, call meetingSession.audioVideo.[removeRealtimeObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/remove-realtime-observer.html).

A RealtimeObserver has the following methods:

* [onVolumeChanged](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-volume-changed.html): called when one or more attendees' volume levels change
* [onSignalStrengthChanged](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-signal-strength-changed.html): called when one or more attendees' signal strengths change
* [onAttendeesJoined](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-attendees-joined.html): called when one or more attendees join the meeting
* [onAttendeesDropped](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-attendees-dropped.html): called when one or more attendees drop from the meeting
* [onAttendeesLeft](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-attendees-left.html): called when one or more attendees leave the meeting
* [onAttendeesMuted](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-attendees-muted.html): called when one or more attendees become muted
* [onAttendeesUnmuted](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/on-attendees-unmuted.html): called when one or more attendees become unmuted

Note that only attendees whose volume level, mute state, or signal strength has changed will be 
included. All callbacks provide both the attendee ID and external user ID from [chime:CreateAttendee](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html) 
so that you may map between the two IDs.

### 6b. Register an active speaker observer (optional)

If you are interested in detecting the active speaker (e.g. to display the active speaker's video 
as a large, central tile), implement an [ActiveSpeakerObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector/-active-speaker-observer/index.html) and register the observer with 
the audio video facade.

You will also need to provide an [ActiveSpeakerPolicy](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy/-active-speaker-policy/index.html). You can use [DefaultActiveSpeakerPolicy](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy/-default-active-speaker-policy/index.html)
or implement the ActiveSpeakerPolicy interface to customize the policy.

To add an ActiveSpeakerObserver, call meetingSession.audioVideo.[addActiveSpeakerObserver(policy, observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector/-active-speaker-detector-facade/add-active-speaker-observer.html).

To remove an ActiveSpeakerObserver, call meetingSession.audioVideo.[removeActiveSpeakerObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector/-active-speaker-detector-facade/remove-active-speaker-observer.html).

An ActiveSpeakerObserver has the following methods:

* [onActiveSpeakerDetected](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector/-active-speaker-observer/on-active-speaker-detected.html): called when one or more active speakers have been detected
* [onActiveSpeakerScoreChanged](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector/-active-speaker-observer/on-active-speaker-score-changed.html): called when active speaker scores change at a given interval

You can control `onActiveSpeakerScoreChanged`'s interval by providing a value for 
`scoreCallbackIntervalMs` while implementing ActiveSpeakerPolicy. You can prevent this callback
from occurring by using a null value.

## 7. Mute and unmute audio

To mute the local attendee's audio, call meetingSession.audioVideo.[realtimeLocalMute()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-local-mute.html).

To unmute the local attendee's audio, call meetingSession.audioVideo.[realtimeLocalUnmute()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-local-unmute.html).

## 8. Share and display video

You can use the following methods in order to send, receive, and display video.

A [VideoTile](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile/index.html) is a binding of a tile ID, an attendee ID, that attendee's video, and a video view. 
The [VideoTileState](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-state/index.html) will contain further information such as if the video tile is for the local 
attendee. Video tiles start without a video view bound to it.

You can view content share the same way that you view a remote attendee's video. The video tile 
state will contain additional information to distinguish if that video tile is for content share.

### 8a. Sending video

Video permissions are required for sending the local attendee's video.

To start sending the local attendee's video, call meetingSession.audioVideo.[startLocalVideo()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start-local-video.html).

To start sending video with a local video configuration, call meetingSession.audioVideo.startLocalVideo(config).

To start sending video with a provided custom `VideoSource`, call meetingSession.audioVideo.startLocalVideo(source). 

To stop sending the local attendee's video, call meetingSession.audioVideo.[stopLocalVideo()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/stop-local-video.html).

### 8b. Getting and switching video device

When starting the local attendee's video, the underlying media client will use the active video 
device or the front facing camera if there is no active video device. You can use the following
methods to get or switch the active video device.

To get the active video device, call meetingSession.audioVideo.[getActiveCamera()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/get-active-camera.html).

To switch the active video device, call meetingSession.audioVideo.[switchCamera()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-device-controller/switch-camera.html).

### 8c. Receiving video

To start receiving video from remote attendees, call meetingSession.audioVideo.[startRemoteVideo()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start-remote-video.html).

To stop receiving video from remote attendees, call meetingSession.audioVideo.[stopRemoteVideo()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/stop-remote-video.html).

### 8d. Adding a video tile observer

You will need to implement a [VideoTileObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/index.html) and register the observer with the audio video 
facade to receive video tile events for displaying video.

To add a VideoTileObserver, call meetingSession.audioVideo.[addVideoTileObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/add-video-tile-observer.html).

To remove a VideoTileObserver, call meetingSession.audioVideo.[removeVideoTileObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/remove-video-tile-observer.html).

A VideoTileObserver has the following methods:

* [onVideoTileAdded](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-added.html): called when an attendee starts sharing video
* [onVideoTileRemoved](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-removed.html): called when an attendee stops sharing video
* [onVideoTilePaused](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-paused.html): called when a video tile's pause state changes from Unpaused
* [onVideoTileResumed](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-resumed.html): called when a video tile's pause state changes to Unpaused
* [onVideoTileSizeChanged](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-size-changed.html): called when a video tile's content size changes

A pause or resume event can occur when the underlying media client pauses the video tile for 
connection reasons or when the pause or resume video tile methods are called.

The video tile state is represented by a [VideoPauseState](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-pause-state/index.html) that describes if and why (e.g., paused by user request, or paused for poor connection) it was paused.

### 8e. Binding a video tile to a video view

To display video, you will also need to bind a video view to a video tile. Create a 
[VideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-render-view/index.html) and bind that view to the video tile in VideoTileObserver's `onVideoTileAdded` 
method. You can use [DefaultVideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-default-video-render-view/index.html) or customize the behavior by implementing the
VideoRenderView interface.

To bind a video tile to a view, call meetingSession.audioVideo.[bindVideoView(videoView, tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/bind-video-view.html).

To unbind a video tile from a view, call meetingSession.audioVideo.[unbindVideoView(tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/unbind-video-view.html).

### 8f. Pausing a remote video tile

To pause a remote attendee's video tile, call meetingSession.audioVideo.[pauseRemoteVideoTile(tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/pause-remote-video-tile.html).

To resume a remote attendee's video tile, call meetingSession.audioVideo.[resumeRemoteVideoTile(tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/resume-remote-video-tile.html).

### 8g. In-depth look and comparison between video APIs

#### Start/Stop Local Video

As the names suggest, `startLocalVideo()` and `stopLocalVideo()` will start and stop sending local attendee’s video respectively. `startLocalVideo()` will first check if the video client is initialized and the application has permission to access device camera, then it will update the service type of video client and turn on its *sending* mode, so that local video can be sent to server and forwarded to other peers.

You should call `startLocalVideo()` when you want to start sending local video to others, and call `stopLocalVideo()` when you want to stop sending local video to others (e.g. when the app is in background). Stopping local video will save uplink bandwidth and computation resource for encoding video.

#### Start/Stop Remote Video

Similar to `startLocalVideo()` and `stopLocalVideo()`, `startRemoteVideo()` and `stopRemoteVideo()` will start and stop receiving remote attendees’ videos by setting the service type of video client and turning on/off its *receiving* mode.

Note that when the application calls `stopRemoteVideo()`, all the resources in the render pipeline will be released, and it takes some time to re-initialize all these resources. You should call `stopRemoteVideo()` only when you want to stop receiving remote videos for a *considerable amount of time*. If you want to temporarily stop remote videos, consider iterating through remote videos and call `pauseRemoteVideoTile(tileId)` instead.

#### Pause/Resume Remote Video Tile

You can call `pauseRemoteVideoTile(tileId)` to temporarily pause a remote video stream. When you call `pauseRemoteVideoTile(tileId)`, the video client will add the tile id to a local block list, so that server will not forward video frames from the paused stream to the client. In that case, you can save downlink bandwidth and computation resource for decoding video. You can call `pauseRemoteVideoTile(tileId)` to remove a video from the local block list and resume streaming.

You should call `pauseRemoteVideoTile(tileId)` when you want to temporarily pause remote video (e.g. when user is not in the video view and remote videos are not actively being showed), or when you want to stop *some* remote videos (e.g. in the video pagination example in the following sections, where we want to render videos on the current page, but stop invisible videos on other pages).

#### Bind/Unbind Video View

To display video, you need to bind a video tile to a video view. You can call `bindVideoView(videoView, tileId)` to bind a video tile to a view, and call `unbindVideoView(tileId)` to unbind a video tile from a view.

Note that bind/unbind only impacts *UI layer*, which means these APIs only control when and where the video stream will be displayed in your application UI. If you unbind a video tile, the server will keep sending video frames to the client, and the client will continue consuming resources to receive and decode video. If you want to reduce unnecessary data consumption and CPU usage of your application by stopping remote videos, you should call `pauseRemoteVideoTile(tileId)` first.

### 8h. Rendering selected remote videos

You can render some of the available remote videos instead of rendering them all at the same time.

To selectively render some remote videos, call meetingSession.audioVideo.[resumeRemoteVideoTile(tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/resume-remote-video-tile.html) on the videos you care about, and call meetingSession.audioVideo.[pauseRemoteVideoTile(tileId)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/pause-remote-video-tile.html) to pause other videos.

See the [Video Pagination with Active Speaker-Based Policy](video_pagination.md) guide for more information about related APIs and sample code.

## 9. Receiving metrics (optional)

You can receive events about available audio and video metrics by implementing a [MetricsObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.metric/-metrics-observer/index.html)
and registering the observer with the audio video facade. Events occur on a one second interval.

To add a MetricsObserver, call meetingSession.audioVideo.[addMetricsObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/add-metrics-observer.html).

To remove a MetricsObserver, call meetingSession.audioVideo.[removeMetricsObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/remove-metrics-observer.html).

A MetricsObserver has the following method:

* [onMetricsReceived](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.metric/-metrics-observer/on-metrics-received.html): called when audio/video related metrics are received

## 10. Sending and receiving data messages (optional)
Attendees can broadcast small (2KB max) data messages to other attendees. Data messages can be used 
to signal attendees of changes to meeting state or develop custom collaborative features. Each message
is sent on a particular topic, which allows you to tag messages according to their function to make it easier to handle messages of different types.

To send a message on a given topic, meetingSession.audioVideo.[realtimeSendDataMessage(topic, data, lifetimeMs)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-send-data-message.html).
When sending a message, the media server stores the messages for the duration specified by `lifetimeMs`. Up to 1024 messages may be stored for a maximum of 5 minutes. Any attendee joining late 
or reconnecting will automatically receive the messages in this buffer once they connect. You can use 
this feature to help paper over gaps in connectivity or give attendees some context into messages that were recently received.

To receive messages on a given topic, implement a [DataMessageObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/add-realtime-data-message-observer.html)
and subscribe it to the topic using meetingSession.audioVideo.[addRealtimeDataMessageObserver(topic, observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/add-realtime-data-message-observer.html). 
Through [onDataMessageReceived](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime.datamessage/-data-message-observer/on-data-message-received.html) method in the observer, you receive a [DataMessage](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime.datamessage/-data-message/index.html) 
containing the payload of the message and other metadata about the message.

To unsubscribe all `DataMessageObserver`s from the topic, call meetingSession.audioVideo.[removeRealtimeDataMessageObserverFromTopic(topic)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/remove-realtime-data-message-observer-from-topic.html).

If you send too many messages at once, your messages may be returned to you with the [throttled](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime.datamessage/-data-message/throttled.html) 
flag set. If you continue to exceed the throttle limit, the server may hang up the connection.

Note: You can only send and receive data messages after calling meetingSession.audioVideo.[start()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start.html). To avoid missing messages, subscribe the `DataMessageObserver` to the topic prior to starting audio video.

## 11. Using Amazon Voice Focus (optional)
Amazon Voice Focus reduces the sound levels of noises that can intrude on a meeting, such as:

- **Environment noises** – wind, fans, running water.
- **Background noises** – lawnmowers, barking dogs.
- **Foreground noises** – typing, papers shuffling.

*Note:* Amazon Voice Focus doesn't eliminate those types of noises; it reduces their sound levels. To ensure privacy during a meeting, call meetingSession.audioVideo.[realtimeLocalMute()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-local-mute.html) to silence yourself.

You must start the audio session before enabling/disabling Amazon Voice Focus or before checking if Amazon Voice Focus is enabled. To enable/disable Amazon Voice Focus, call call meetingSession.audioVideo.[realtimeSetVoiceFocusEnabled(enabled)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-set-voice-focus-enabled.html). To check if Amazon Voice Focus is enabled, call [meetingSession.audioVideo.realtimeIsVoiceFocusEnabled()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-is-voice-focus-enabled.html).

When Amazon Voice Focus is running, a CPU usage increase is expected, but the performance impact is small on modern devices (on average, we observed around 5-7% CPU increase). If your app will be running on resource-critical devices, you should take this into consideration before enabling Amazon Voice Focus.

Note that if you want to share music or background sounds with others in the call (e.g., in a fitness or music lesson application), you should disable Amazon Voice Focus. Otherwise, the Amazon Chime SDK will filter these sounds out.

## 12. Using a custom video source, sink or processing step (optional)

Builders using the Amazon Chime SDK for video can produce, modify, and consume raw video frames transmitted or received during the call. You can allow the facade to manage its own camera capture source, provide your own custom source, or use a provided SDK capture source as the first step in a video processing pipeline which modifies frames before transmission. See the [Custom Video Sources, Processors, and Sinks](custom_video.md) guide for more information.

## 13. Share screen and other content (optional)

Builders using the Amazon Chime SDK for Android can share a second video stream such as screen capture in a meeting without disrupting their applications existing audio/video stream. When a content share is started, another attendee with the attendee ID `<attendee-id>#content` joins the meeting. You can subscribe its presence event to show it in the roster and bind its video tile to a video render view the same as you would for a regular attendee.

Each attendee can share one content share in addition to their main video. Each meeting may have two simultaneous content shares. Content share does not count towards the max video tile limit.

### 13a. Start and stop the content share

To start the content share, call meetingSession.audioVideo.[startContentShare(source)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-controller/start-content-share.html).

To stop the content share, call meetingSession.audioVideo.[stopContentShare()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-controller/stop-content-share.html).

### 13b. Register a content share observer

You can receive events about the content share by implementing a [ContentShareObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-observer/index.html).

To add a ContentShareObserver, call meetingSession.audioVideo.[addContentShareObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-controller/add-content-share-observer.html).

To remove a ContentShareObserver, call meetingSession.audioVideo.[removeContentShareObserver(observer)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-controller/remove-content-share-observer.html).

You can implement the following callbacks:

* [onContentShareStarted](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-observer/on-content-share-started.html): called when the content share has started

* [onContentShareStopped(status)](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-observer/on-content-share-stopped.html): called when the content is no longer shared with other attendees with the reason provided in the status

### 13c. Collect content share metrics

You will receive content share metrics if you registered a metric observer by [9.Receiving metrics (optional)](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md#9-receiving-metrics-optional).

Content share metrics will be prefixed by `contentShare`. Or you can use the [isContentShareMetric()](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.metric/-observable-metric/is-content-share-metric.html) to help identify a content share metric.

## 14. Configuring Remote Video Subscriptions (optional)

Amazon Chime SDK allows builders to have complete control over the remote videos received by each of their application’s end-users. This can be accomplished using the API [AudioVideoFacade.updateVideoSourceSubscriptions](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-default-audio-video-facade/update-video-source-subscriptions.html). See [Configuring Remote Video Subscriptions](configuring_remote_video_subscription.md) for more information.
