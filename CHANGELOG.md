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
