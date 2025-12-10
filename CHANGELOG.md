## [0.25.2] - 2025-12-09

### Changed
* Guard video client stop with a mutex

### Added
* Added following meeting events: contentShareStartRequested/contentShareStarted/contentShareStopped/contentShareFailed/voiceFocusEnabled/voiceFocusDisabled/voiceFocusEnableFailed/voiceFocusDisableFailed/videoCaptureSessionInterruptionBegan/videoCaptureSessionInterruptionEnded/networkConnectionTypeChanged/signalingDropped/iceGatheringCompleted
* Added following meeting event attributes: voiceFocusErrorMessage/lowPowerModeEnabled/networkConnectionType/signalingOpenDurationMs/iceGatheringDurationMs

## [0.25.1] - 2025-10-02

### Added
* Added following meeting events: meetingReconnected/audioInputFailed/videoClientSignalingDropped/contentShareSignalingDropped/appStateChanged/appMemoryLow/contentShareStartRequested/contentShareStarted/contentShareStopped/contentShareFailed/voiceFocusEnabled/voiceFocusDisabled/voiceFocusEnableFailed/voiceFocusDisableFailed/videoCaptureSessionInterruptionBegan/videoCaptureSessionInterruptionEnded
* Added following meeting event attributes: meetingReconnectDurationMs/audioInputErrorMessage/signalingDroppedErrorMessage/appState/batteryLevel/batteryState/contentShareErrorMessage/voiceFocusErrorMessage/lowPowerModeEnabled

## [0.25.0] - 2025-06-17

### Changed
* Compile and target Android SDK 35; also update gradle, AGP, and dependencies

## [0.24.1] - 2025-03-28

### Fixed
* Fixed some broken links in the guides
* Fixed a bug preventing cleanup after joining from another device and added unit tests.

## [0.24.0] - 2025-02-20

### Fixed
* When meeting has ended normally, but not initiated by the client, send a meetingEnded event rather than a meetingFailed event. 

### Changed
* Upgrade gradle version and dokka

## [0.23.0] - 2024-08-15

### Changed
* Updated SDK compile and target SDK level to 34 (Android 14)
  * Changed resize logic for handling screen rotation in `DefaultScreenCaptureSource` to avoid restart the capturing, which lead to security exception
  * Registered required `MediaProjection.Callback` before creating the virtual display
  * [Demo] Declaimed required permissions for foreground service in manifest file
  * [Demo] Fixed memory leak for `ScreenShareServiceConnection` after screen rotations
  * [Demo] Added `microphone` foreground service for app to still capture microphone audio after being backgrounded

## [0.22.0] - 2024-07-18

### Added
* Support configurable reconnecting timeout.

## [0.21.0] - 2024-06-18

### Added
* Added non-functional x86 stubs. Projects can now build Arm and x86 targets, which may be useful if bundling an app. x86 targets will not function and are not intended to be installed or run on any x86 device or emulator.
  **Important: Only Arm devices are supported.**

## [0.20.1] - 2024-05-16

## [0.20.0] - 2024-03-21

### Added
* Added `AudioDeviceCapabilities` to `AudioVideoConfiguration`, which allows configuring whether the audio input and output devices are enabled or disabled before starting a meeting.
  * Audio recording permissions will only be required when using `AudioDeviceCapabilities.InputAndOutput`
  * [Demo] Added spinner to join screen to configure the audio device capabilities

### Removed
* **Breaking** Removed `AudioMode.NoDevice`, which is now replaced by `AudioDeviceCapabilities.None`. Apps which previously used `AudioMode.NoDevice` can achieve the same functionality by using `AudioDeviceCapabilities.None` when constructing an `AudioVideoConfiguration`, e.g. `AudioVideoConfiguration(audioDeviceCapabilities = AudioDeviceCapabilities.None)`.

## [0.19.1] - 2024-02-15

### Fixed

* Move BackgroundFilterVideoFrameProcessor.filterByteBuffer to local variable in getProcessedFrame() to prevent race condition

## [0.19.0] - 2023-12-20

### Added

* Add support for high-definition WebRTC sessions with 1080p webcam video and 4K screen share, and decode support for VP9. Developers can choose video encoding bitrates up to 2.5Mbps, frame rates up to 30fps.
* Add a new alternative media binary `amazon-chime-sdk-media-no-video-codecs` that excludes software video codecs. This can be used to replace `amazon-chime-sdk-media` if developers do not need video and content share functionality, or software video codec support.

## [0.18.3] - 2023-09-28

### Added
* Support sending and receiving redundant audio data to help reduce the effects of packet loss on audio quality. See README for more details.

## [0.18.2] - 2023-06-27

## [0.18.1] - 2023-05-16

### Added
* Pass client UTC offset to audio and video client for metrics.

## [0.18.0] - 2023-03-16

### Changed
* **Breaking** Updated the Ingestion related APIs / classes to support generic attributes, no changes required if not using custom `EventClientConfiguration` and the following classes.
  * Changed `SDKEvent.eventAttributes` from `EventAttributes` to String-keyed map
  * Added `tag`, `metadataAttributes` to `EventClientConfiguration`

## [0.17.10] - 2023-01-26

### Added
* Added additional session statuses for audio device I/O timeouts.

### Fixed
* [Demo] Fixed the remote video render delay


## [0.17.9] - 2022-12-02

## [0.17.8] - 2022-11-16

## [0.17.7] - 2022-10-20

### Fixed
* Fixed bugs that occured at video capacity
* [Demo] Updated demo to use new functionality to prevent camera from toggling at video limit
* Fixed background filtered video rendering before joining a meeting session
* Fixed memory leak issue in rgba video rendering

## [0.17.6] - 2022-08-25

### Fixed
* Gracefully deserialize malformed ingestion events JSON data in local database to prevent crashes.

## [0.17.5] - 2022-08-12

### Added
* Added support to set max bit rate for local video and content share
* [Demo] Add video configuration options to set max bit rate for local video in meeting

### Fixed
* Use thread safe mutableSet for observers

## [0.17.4] - 2022-07-28

### Changed
* Changed `listSupportedVideoCaptureFormats` to find max fps from device
* Added `AudioRecordingPresetOverride` to allow builders to pass in openSL ES recording preset configuration incase the default used by us does not work for some android devices.

## [0.17.3] - 2022-07-14

## [0.17.2] - 2022-06-17

### Fixed
* Fixed background blur and replacement pixelated video on desktop. (Issue https://github.com/aws-samples/amazon-chime-react-native-demo/issues/145)
* Updated unit tests according to above fix
* Load model only when frame dimensions change
* Fixed crash from openGL due to potential race condition.
* [Demo] Fixed videos paused when change from other tabs to video tab and reopen app from background
* [Demo] Fixed screen sharing is unavailable to present videos
* [Demo] Fixed demo application not subscribing remote videos properly

## [0.17.1] - 2022-06-03

## [0.17.0] - 2022-05-18

### Added
* Added support for background blur and replacement video filter processor. See [background video filters](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/background_video_filters.md) for more details.
* [Demo] Adder two new video filters - background blur and replacement.

## [0.16.0] - 2022-05-11

### Added
* Added `audioStreamType` in `AudioVideoConfiguration` for supporting audio stream configuration.

### Fixed
* Fixed calling start multiple times without stop crashes video in some phones. (Issue #356)

## [0.15.4] - 2022-04-21

## [0.15.3] - 2022-04-07

### Added
* Added [replicated meeting guide](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/replicated_meetings.md).

### Fixed
* Added proper call of demotion callback on audio or video disconnection.

## [0.15.2] - 2022-03-21

### Added
* Added support for media replication to link multiple WebRTC media sessions together to reach larger and global audiences. Participants connected to a replica session can be granted access to join the primary session and can switch sessions with their existing WebRTC connection.

## [0.15.1] - 2022-03-10

### Fixed
* Catch and ignore the exception from rendering one video frame and move on to the next. This helps workaround a openGL error on some Android 12 devices at initial rendering phase.
* [Demo] Added overridden endpoint url capability to live transcription API.

### Added
* Added support to live transcription for new features including personally identifiable information content identification and redaction, partial results stabilization, custom language models, and language identification for Amazon Transcribe and PHI content identification for Amazon Transcribe Medical.
* [Demo] Added language identification configuration for live transcription API.

## [0.15.0] - 2022-02-24

### Added
* Added the meetingStartDurationMs event in ingestionEvents to record the time that elapsed between the start request and the beginning of the meeting.
* Added priority based downlink policy to control the way how a recipient subscribes to the remote video sources

## [0.14.3] - 2022-02-10

## [0.14.2] - 2022-01-27

### Added
* Added Maven Support for Android SDK.

### Fixed
* Fixed out of order logging on `TextureRenderView`.
* Fixed Maven release process error and skipped 0.14.1

## [0.14.0] - 2021-12-21

### Added
* Added APIs for Audio Video configuration i.e `AudioVideoConfiguration` to be used during a meeting session.
* Added support for joining meetings using one of `AudioMode.Mono16K`, `AudioMode.Mono48K` and `AudioMode.Stereo48K` audio modes.
* **Breaking** The `AudioMode.Stereo48K` will be set as the default audio mode if not explicitly specified when starting the audio session. Earlier, Mono/16KHz audio was the default and the only audio mode supported.
* [Demo] Added ways to join a meeting using various audio modes.

### Fixed
* Fixed crash when rgba video frames are sent to DefaultVideoRenderView directly in preview use case.

## [0.13.1] - 2021-11-11

### Fixed
* [Demo] Fixed demo app crashes when screen share is off and then leave meeting.

### Changed
* Changed the silence threshold to 0.2 from 0.0 for `DefaultActiveSpeakerPolicy` (Issue #259) to be more consistent with other platform.
* Expose weights/rates/thresholds to `DefaultActiveSpeakerPolicy` constructor to make builders easier to customize `DefaultActiveSpeakerPolicy`.

## [0.13.0] - 2021-11-01

### Added
* Supports integration with Amazon Transcribe and Amazon Transcribe Medical for live transcription. The Amazon Chime Service uses its active talker algorithm to select the top two active talkers, and sends their audio to Amazon Transcribe (or Amazon Transcribe Medical) in your AWS account. User-attributed transcriptions are then sent directly to every meeting attendee via data messages. Use transcriptions to overlay subtitles, build a transcript, or perform real-time content analysis. For more information, visit [the live transcription guide](https://docs.aws.amazon.com/chime/latest/dg/meeting-transcription.html).

* [Demo] Added meeting captions functionality based on the live transcription APIs. You will need to have a serverless deployment to create new AWS Lambda endpoints for live transcription. Follow [the live transcription guide](https://docs.aws.amazon.com/chime/latest/dg/meeting-transcription.html) to create necessary service-linked role so that the demo app can call Amazon Transcribe and Amazon Transcribe Medical on your behalf.

## [0.12.0] - 2021-09-02

### Added
* [Demo] Added binding to screen share service to avoid condition where screen share starts before service.

### Fixed
* Fixed an issue where `DefaultCameraCaptureSource` cannot be used without `DefaultMeetingSession` (Issue #309)

### Changed
* **Breaking** Changed the behavior of `DefaultScreenCaptureSource` to better handle failure cases (Issue #317).
  - Previously errors with getting the media projection either resulted in a SecurityException or silently failed.
  - Now `onCaptureFailed` from `CaptureSourceObserver` will be called to handle errors with getting the media projection.
* Updated targetSdkVersion from 28 to 30

## [0.11.6] - 2021-07-21

### Fixed
* Fixed Screen share rotation issue (Issue #289)
* [Demo] Fixed crash when end meeting while screen sharing

## [0.11.5] - 2021-06-24

### Added
* Added events ingestion to report meeting events to Amazon Chime backend.

## 2021-05-10

### Fixed
* [Documentation] Fixed documentation to say `Amazon Voice Focus` instead of `voice focus`
* [Documentation] Added sample code to the meeting event guide

## [0.11.4] - 2021-04-14

### Changed
* Disabled simulcast for P2P calls, which helps improving video quality of two-party meetings.

## [0.11.3] - 2021-04-02

### Fixed
* [Demo] Fixed video flickering issue when active speakers were detected by using `DiffUtil` instead of `notifyDataSetChanged` to update the video adapter.
* Fixed `DefaultDeviceController` not passing correct route for USB headset.

## [0.11.2] - 2021-03-04

### Changed
* Enabled send-side bandwidth estimation in video client, which improves video quality in poor network conditions.

### Fixed
* Fixed `CreateMeetingResponse` and `MeetingSessionConfiguration` to have nullable `externalMeetingId` since this is not required.

### Added
* Added additional constructor of `MeetingSessionConfiguration` to create it without `externalMeetingId`

## [0.11.1] - 2021-02-24

### Added
* Added `initializeAudioClientAppInfo` to `AppInfoUtil` for use with audio client.
* Added support for `TYPE_USB_HEADSET` in `DefaultDeviceController` for cases like USB-C headphone.
* Added `AUDIO_USB_HEADSET` as a new enum in `MediaDeviceType`.

### Fixed
* Fixed `DefaultCameraCaptureSource`, `DefaultSurfaceTextureCaptureSource` concurrency issue (Issue #221).

## [0.11.0] - 2021-02-04

### Added
* Added Analytics
    * `EventAnalyticsController`, `EventAnalyticsFacade`, `EventAnalyticsObserver` to handle analytics.
    * Added `EventAttributes`, `EventName`, `MeetingHistoryEventName` for meeting event information.
    * Added `externalMeetingId` to property of `MeetingSessionConfiguration`.
    * Added `PermissionError` to `CaptureSourceError`.
    * **Breaking** Added `eventAnalyticsController` to property of `AudioVideoFacade`.
    * [Demo] Added `PostLogger` to demo application to showcase sending events to backend.
    * [Documentation] Added analytics API usage documentation.

### Changed
* Analytics
  * **Breaking** Changed to take `EventAnalyticsController` as an additional parameter of `DefaultAudioVideoFacade` constructor.
  * Changed to take `EventAnalyticsController` as an additional parameter of `DefaultAudioClientObserver`, `DefaultAudioClientController` constructor.

### Fixed
* Fixed a case when front camera is missing in the phone. (Issue #218)

## [0.10.0] - 2021-01-21

### Added
* **Breaking** Added content share metrics as new enums in `ObservableMetric`.
* Added content share APIs that supports a 2nd video sending stream such as screen capture, read [content share guide](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/content_share.md) for details.
* Added `minFps` to `SurfaceTextureCaptureSource` as a property to help encoder improve the quality.
* Added `createContentShareMeetingSessionConfiguration` as helper function in `MeetingSessionConfiguration` to generate content configuration based on the existing session.
* Added screen share feature in demo app.
* Added message for video tiles paused by poor network in demo app.
* Added logic to `stopRemoteVideo`/`startRemoteVideo` when application is background/foregrounded to save network bandwidth in demo app.
* Added TURN uris received callback.
* [Documentation] Added usage documentation.

### Changed
* **Breaking** `AudioVideoFacade` now also implements `ContentShareController`.
* **Breaking** Changed to take `ContentShareController` as an additional parameter of `DefaultAudioVideoFacade` constructor.
* Changed AudioManager mode to be `MODE_IN_COMMUNICATION` only after builders call `audioVideo.start()`.
* Update text of additional options on demo app.
* Changes that support a speed up of video client initialization. `requestTurnCreds` callback will only be invoked as a backup for media layer logic. The signaling url is now passed into video client start. A new callback `onTurnURIsReceived` will be invoked when TURN uris are received by the client. This allows urls to be modified with urlRewriter or custom builder logic.
* Changed the demo app behavior so that when user starts/stops local video, the app does not switch to the Video tab automatically.

### Fixed
* Fixed potential concurrency issue on `VideoSourceAdapter`.
* Fixed a video connection issue on network where DNS 8.8.8.8 is blocked. `ACCESS_NETWORK_STATE` permission is required to discover available DNS on the network.

## [0.9.1] - 2021-01-08

### Fixed
* Fix a bug that internal capture source was not stopped properly when the video client was being stopped.
* Fix camera capture start failure on certain Android devices when there are no FPS ranges at or below desired FPS max.

## [0.9.0] - 2020-12-17

### Added
* Added video pagination feature in the Android demo app. Remote videos will be paginated into several pages. Each page contains at most 4 videos, and user can switch between different pages. Videos that are not being displayed will not consume any network bandwidth or computation resource.
* Added active speaker-based video tile feature in the Android demo app. Video tiles of active speakers will be promoted to the top of the list automatically.

### Fixed
* Fixed a demo app issue that `SurfaceView` was not cleared up correctly when switching between Screen tab and Video tab.
* Fixed a demo app issue that `mirror` property was not reset when `VideoHolder` is recycled.
* Fixed rotation issue in demo app.

### Changed
* Refactored video view to resemble iOS UI so that video doesn't get cropped.
* **Breaking** Remove the internal video tile mapping entry not only when the video is *unbound*, but also when the video is *removed*. This fixes [`onVideoTileAdded(tileState)` is sometimes not called issue](https://github.com/aws/amazon-chime-sdk-android/issues/186), and provides better API symmetry so that builders no longer need to call `unbindVideoView(tileId)` if they did not call `bindVideoView(videoView, tileId)`.
  * After this fix, the internal video tile mapping entry will be removed before `onVideoTileRemoved(tileState)` callback is called. Please check your `VideoTileObserver`s and make sure your `onVideoTileRemoved(tileState)` handlers do not call any SDK APIs that depend on the existance of video tiles (e.g. `bindVideoView(videoView, tileId)`).

## [0.8.2] - 2020-12-11

## [0.8.1] - 2020-11-20

## [0.8.0] - 2020-11-17

### Custom Video Source
This release includes support for custom video sources, and therefore includes a lot of additional APIs.  Though most builders who have not been making modifications to internal render path should not need to make any changes and will not hit any compile time issues, there are some small breaking changes to certain render path APIs to ensure consistency within data flow across the SDK.  Recommendations are below, and all breaking changes should result in compile time errors (if you implemented the class yourself), if you continue to have difficulty resolving issues please cut an issue.

### Added
* Added new APIs in `RealtimeControllerFacade` to enable/disable Amazon Voice Focus (ML-based noise suppression) and get the on/off status of Amazon Voice Focus.
* Added Amazon Voice Focus feature in Android demo app.
* Added `VideoFrame`, `VideoRotation`, `VideoContentHint`, `VideoFrameBuffer`, `VideoFrameI420Buffer`, `VideoFrameRGBABuffer`, `VideoFrameTextureBuffer` classes, enums, and interfaces to hold video frames of various raw types.
* Added `VideoSource` and `VideoSink` to facilitate transfer of `VideoFrame` objects.
* Added `CameraCaptureSource`, `CaptureSourceError`, `CaptureSourceObserver`, `VideoCaptureFormat`, and `VideoCaptureSource` interfaces and enums to facilitate releasing capturers as part of the SDK.
* Added `DefaultCameraCaptureSource` implementation of `CameraCaptureSource`.
* Added `EglCore`, `DefaultEglCore`, `EglCoreFactory`, `DefaultEglCoreFactory`, `GlVideoFrameDrawer`, `DefaultGlVideoFrameDrawer`, `SurfaceTextureCaptureSource`, `DefaultSurfaceTextureCaptureSource`, `SurfaceTextureCaptureSourceFactory`, `DefaultSurfaceTextureCaptureSourceFactory`, for shared graphics related needs.
* Added `listVideoDevices` and `listSupportedVideoCaptureFormats` to `MediaDevice.Companion`.
* Added `SurfaceRenderView` and `TextureRenderView` an open source implementation of rendering onto a `SurfaceView` and `TextureView` respectively.
* Added `VideoLayoutMeasure` and `EglRenderer` + `DefaultEglRenderer` internal helper classes for use within aforementioned render views.
* Added more verbose logging from media layer to SDK layer for builders to control log level. Set `LogLevel` to `INFO` or above for production application to not be bombarded with logs..
* Added `getActiveAudioDevice` in `DefaultDeviceController` for API 24 or greater for developers to get currently used device

### Changed
* The render path has been changed to use `VideoFrame`s for consistency with the send side, this includes:
  * **Breaking** `VideoTileController.onReceiveFrame` now takes `VideoFrame?` instead of `Any?`.
    * Builders with a custom `VideoTileController` will have to update APIs correspondingly.
    * Currently the buffer type of all frames coming from the AmazonChimeSDKMedia library should be `VideoFrameI420Buffer` which should have the same API as the legacy, closed source buffer used.
  * **Breaking** `VideoTileController.initialize` and `VideoTileContoller.destroy` have been removed due to not being used.
    * Builders with a custom `VideoTileController` will have to remove APIs.
    * Previously these functions were called by internal `VideoClientController`, but now they no longer share state; if custom implementations need initialization, it must be done externally.
  * **Breaking** `VideoTile.renderFrame` now takes `VideoFrame` instead of `Any` and has been replaced by extending `VideoSink` and using `onReceivedVideoFrame`.
    * Builders with a custom `VideoTile` will have to update APIs correspondingly.
    * Currently the buffer type of all frames coming from the AmazonChimeSDKMedia library should be `VideoFrameI420Buffer` which should have the same API as the legacy, closed source buffer used.
  * **Breaking** `VideoTile.bind` no longer takes `bindParams: Any?` which was being used to inject `EGLContext` from internal classes.
    * Builders with a custom `VideoTile` will have to update APIs correspondingly, and add bind parameters outside of interface functions if necessary.
  * **Breaking** `VideoRenderView` is now just a `VideoSink` (i.e. it now accepts `VideoFrame` object via `VideoSink.onReceivedVideoFrame` rather then `Any?` via `render`).
    * Builders with a custom `VideoTile` will have to update APIs correspondingly.
    * Currently the buffer type of all frames coming from the AmazonChimeSDKMedia library should be `VideoFrameI420Buffer` which should have the same API as the legacy, closed source buffer used.
  * **Breaking** `VideoRenderView.initialize` and `VideoRenderView.finalize` have been abstracted away to `EglVideoRenderView` and are now named `init` and `release` respectively.
  * **Breaking** `DefaultVideoRenderView.setMirror` is now a class variable `mirror`.  `DefaultVideoRenderView.setScalingType` is now a class variable `scalingType`, and takes `VideoScalingType`
  * `DefaultVideoRenderView` now inherits from `SurfaceTextureView`.
* If no custom source is provided, the SDK level video client will use a `DefaultCameraCaptureSource` instead of relying on capture implementations within the AmazonChimeSDKMedia library; though behavior should be identical, please open an issue if any differences are noticed..
* Added additional, optional `id` (unique ID) parameter to `MediaDevice` for video capture devices.
* **Breaking** Changed the default log level of `ConsoleLogger` to `INFO` level from `WARN`.

### Fixed
* **Breaking** Changed behavior to no longer call `onVideoTileSizeChanged` when a video is paused to fix a bug where pausing triggered this callback with width=0 and height=0.
* Fix audio issue when using Bluetooth device by changing the sample rate to 16kHz.
* **Breaking** Fixed `listAudioDevices` to stop returning A2DP bluetooth devices, which are one way communication devices such as Bluetooth Speaker
* Fixed `videoTileDidAdd` not being called for paused tiles.

## [0.7.6] - 2020-11-13

## [0.7.5] - 2020-10-23

### Fixed
* Revert structured concurrency which can lead to deadlock

## [0.7.4] - 2020-10-08

### Changed
* Changed `MAX_TILE_COUNT` in the demo app from 4 to 16. Now the demo app can support at most 16 remote video tiles.

## [0.7.3] - 2020-09-10

### Fixed
* Pass correct microphone input value for audio client in `DefaultAudioClientController` for better audio input quality

### Changed
* Replace usage of GlobalScope with structured concurrency

## [0.7.2] - 2020-09-01

### Fixed
* Fixed the issue that `listAudioDevices` does not return built-in handset for some devices 
* Fixed a bug that attendee events got filtered out due to absence of `externalUserId`

## [0.7.1] - 2020-08-13

## [0.7.0] - 2020-07-31

### Added
* **Breaking** Added additional field for `MeetingSessionCredentials`
* Added data message APIs

## [0.6.0] - 2020-07-20

### Added
* **Breaking** Added `isLocalTile` to constructor of `DefaultVideoTile`, `VideoTileFactory` and `VideoTileState`
* Added icons for the demo app
* Added `SplashActivity` in demo app for displaying the launch screen
* Added choose device list in the demo app for speaker button
* Added metrics table in the demo app
* Added `onVideoTileSizeChanged` API in `VideoTileObserver` for video stream content size change
* Added attendee id to local video tile

### Changed
* Changed the theme to support dark theme

## [0.5.3] - 2020-06-24

### Changed
* Updated `DefaultAudioClientController` to teardown bluetooth SCO connection after audioClient is stopped. Also reset AudioManager mode and speakerphone state to what it was before the call when audioClient is stopped. Also added bluetooth SCO connection teardown when audio device other than bluetooth is chosen

## [0.5.1] - 2020-05-22

### Added
* Added `onAttendeesDropped` API in `RealtimeObserver` for attendee who got dropped

## [0.5.0] - 2020-05-14

### Added
* **Breaking** Added additional fields for `CreateAttendeeResponse` and `CreateMeetingResponse`
* Added `Versioning` class and `sdkVersion` API for retrieving current version of SDK
* Added `onAudioSessionDropped` API in `AudioVideoObserver` for temporary disconnects
* Added new parameter `urlRewriter` in `MeetingSessionConfiguration` for customizing url
* Updated demo application to use pause and resume feature

### Changed
* Updated demo app to work with updated [amazon-chime-sdk-js serverless demo](https://github.com/aws/amazon-chime-sdk-js/tree/master/demos/serverless). Note that you will need to redeploy the serverless demo to work with the updated demo app
* Updated methods for `AudioVideoObserver`, `RealtimeObserver`, `DeviceChangeObserver`, `VideoTileObserver`, and `MetricsObserver` to be called on main thread. Make sure to dispatch long-running tasks to another thread to avoid blocking the main thread.

### Fixed
* Fixed bug where `onAudioSessionStarted()` was called twice
* Fixed bug where `stop()` hung when it was called in `onAttendeesLeft()`
* Fixed main thread freezing issue caused by calling `stop()` when in reconnecting state
* Fixed bug where `onAudioSessionStopped()` was not getting called after calling `stop()`
* Fixed an issue that blocked user from removing paused video tiles.

## [0.4.1] - 2020-04-23

### Added
* Added proguard rules for release build type

### Fixed
* Fix bug where external id for self is sometimes empty

### Changed
* Update `DefaultActiveSpeakerPolicy` to use consistent `cutoffThreshold` in all platforms
