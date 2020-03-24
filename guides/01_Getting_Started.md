### Getting Started

This guide contains a quick explanation of initializing the meeting session and using that to 
access audio and video features. For more information, please refer to the API documentation at 
TODO - Add link to API docs or refer to the demo app.

#### Permissions
Before calling the APIs to start audio, the app will need to request the following permissions from the user:

```
Manifest.permission.MODIFY_AUDIO_SETTINGS,
Manifest.permission.RECORD_AUDIO
```

Before calling the APIs to start video, the app will need to reqeust the following permissions from the user:
```
Manifest.permission.CAMERA
```

Calling the APIs without having the above permissions granted will result in a `SecurityException`. 
Amazon Chime SDK already declares these permissions in its manifest file.

#### Getting Meeting Info

The first step is to get various parameters about the meeting. The client application will receive 
this information from the server application. It is up to the builder to decide on how the client
application and server application communicates. 

For testing purposes, you can deploy the serverless demo from [amazon-chime-sdk-js](https://github.com/aws/amazon-chime-sdk-js). 
After the deployment you will have a URL (which this guide will refer to as `server_url`)

To get the meeting info make a POST request to:
```
"${server_url}join?title=$meetingId&name=$attendeeName&region=$MEETING_REGION"
```

These are the parameters to include in the request:
* title: Meeting ID for the meeting to join
* name: Attendee name to join the meeting with
* region: For now you can use "us-east-1"

#### Create MeetingSessionConfiguration

Parse the JSON response obtained from your server application to create the `MeetingSessionConfiguration` object. 

```
val meetingResponse = gson.fromJson(response, MeetingResponse::class.java)
MeetingSessionConfiguration(
    CreateMeetingResponse(meetingResponse.joinInfo.meeting),
    CreateAttendeeResponse(meetingResponse.joinInfo.attendee)
)
```

#### Create MeetingSession

Create the `DefaultMeetingSession` using the `MeetingSessionConfiguration` object.

```
meetingSession =
    DefaultMeetingSession(configuration, logger, applicationContext)
```

#### Access AudioVideoFacade

Get the `AudioVideoFacade` object from the `MeetingSession` Object and call various APIs.

```
private lateinit var audioVideo: AudioVideoFacade
```

```
audioVideo = meetingSession.audioVideo
```

##### Audio

To start audio:
```
audioVideo.start()
```

To stop audio:
```
audioVideo.stop()
```

To listen to AudioClient’s lifecycle events:
```
audioVideo.addAudioVideoObserver(AudioVideoObserver)
```
```
audioVideo.removeAudioVideoObserver(AudioVideoObserver)
```

A class implementing `AudioVideoObserver` would need to implement the following:

```
onAudioSessionStartedConnecting(reconnecting: Boolean)

onAudioSessionStarted(reconnecting: Boolean)

onAudioSessionStopped(sessionStatus: MeetingSessionStatus)

onAudioSessionCancelledReconnect()

onConnectionRecovered()

onConnectionBecamePoor()

onVideoSessionStartedConnecting()

onVideoSessionStarted(sessionStatus: MeetingSessionStatus)

onVideoSessionStopped(sessionStatus: MeetingSessionStatus)
```

To Mute:
```
audioVideo.realtimeLocalMute()
```

To UnMute:
```
audioVideo.realtimeLocalUnmute()
```

To listen to Volume and Signal Strength callbacks:

```
audioVideo.addRealtimeObserver(RealtimeObserver)
```
```
audioVideo.removeRealtimeObserver(RealtimeObserver)
```

A class implementing `RealtimeObserver` would need to implement the following:

```
onVolumeChanged(volumeUpdates: Array<VolumeUpdate>)

onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>)

onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>)

onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>)

onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>)

onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>)
```

VolumeLevel is an enum with the following values: `Muted`, `NotSpeaking`, `Low`, `Medium`, `High`
SignalStrength is an enum with the following values: `None`, `Low`, `High`

To detect active speaker:
```
audioVideo.addActiveSpeakerObserver(ActiveSpeakerPolicy, ActiveSpeakerObserver)
```

A class implementing `ActiveSpeakerObserver` would need to implement the following:

```
val scoreCallbackIntervalMs: Int?

onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>)

onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>)
```

You can also define a logic to determine who are the active speakers by implementing `ActiveSpeakerPolicy` or use the default implementation `DefaultActiveSpeakerPolicy`. 

A class implementing `ActiveSpeakerPolicy` would need to implement the following:
```
calculateScore(attendeeInfo: AttendeeInfo, volume: VolumeLevel): Double
```

##### Devices

To list audio devices:
```
audioVideo.listAudioDevices()
```

To select an audio device to use:
```
audioVideo.chooseAudioDevice(MediaDevice)
```

To listen to audio device changes:
```
audioVideo.addDeviceChangeObserver(DeviceChangeObserver)
```
```
audioVideo.removeDeviceChangeObserver(DeviceChangeObserver)
```

A class implementing `DeviceChangeObserver` would need to implement the following:

```
onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>)
```

##### Metrics

To listen to metrics:
```
audioVideo.addMetricsObserver(MetricsObserver)
```
```
audioVideo.removeMetricsObserver(MetricsObserver)
```

A class implementing `MetricsObserver` would need to implement the following:
```
onMetricsReceived(metrics: Map<ObservableMetric, Any>)
```

##### Video

To start or stop local video:
```
audioVideo.startLocalVideo()
```
```
audioVideo.stopLocalVideo()
```

To start or stop remove video:
```
audioVideo.startRemoteVideo()
```
```
audioVideo.stopRemoteVideo()
```

To switch camera:
```
audioVideo.switchCamera()
```

##### Video Rendering

Video has the following components for rendering video frames to a view:

* VideoTileControllerFacade - Add observer for video tile updates, bind, and unbind a view
* VideoTileObserver - Handles adding and removing video tiles as attendees start / stop sending video
* VideoTile - Contains a view to render frames to (once bound) as well as the state of the tile (VideoTileState)
* VideoRenderView - View to render frames to

To listen to video tile events:
```
audioVideo.addVideoTileObserver(VideoTileObserver)
```
```
audioVideo.removeVideoTileObserver(VideoTileObserver)
```

A class implementing VideoTileObserver would need to implement the following:
```
onVideoTileAdded(tileState: VideoTileState)

onVideoTileRemoved(tileState: VideoTileState)

onVideoTilePaused(tileState: VideoTileState)

onVideoTileResumed(tileState: VideoTileState)
```

To bind views to video tiles:
```
audioVideo.bindVideoView(VideoRenderView, Int)
```
```
audioVideo.unbindVideoView(Int)
```

VideoTileState consists of the following properties:
```
tileId: Int
attendeeId: String?
paused: Boolean
isLocalTile: Boolean
```

VideoRenderView has the following methods:
```
initialize(initParams: Any?)
finalize()
renderFrame(frame: Any)
```
