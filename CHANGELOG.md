## Unreleased

### Changed
* Changed `MAX_TILE_COUNT` in the demo app from 4 to 16. Now the demo app can support at most 16 remote video tiles.

## [0.7.3] - 2020-09-10

### Fixed
* Pass correct value for audio client in `DefaultAudioClientController`

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
