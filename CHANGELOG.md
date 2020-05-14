## Unreleased
### Added
* Added new parameter `urlRewriter` in `MeetingSessionConfiguration` which allows you to customize url
* Updated demo application to use pause and resume feature

### Fixed
* Fixed an issue that blocked user from removing paused video tiles.

## [0.5.0] - 2020-05-08
### Added
* **Breaking** Added additional fields for `CreateAttendeeResponse` and `CreateMeetingResponse`
* Added `Versioning` class and `sdkVersion` API for retrieving current version of SDK
* Added `onAudioSessionDropped` API in `AudioVideoObserver` for temporary disconnects

### Changed
* Updated demo app to work with updated [amazon-chime-sdk-js serverless demo](https://github.com/aws/amazon-chime-sdk-js/tree/master/demos/serverless). Note that you 
need to redeploy the serverless demo to work with the updated demo app
* `AudioVideoObserver`, `RealtimeObserver`, `DeviceChangeObserver`, `VideoTileObserver`, and `MetricsObserver`'s methods will be called on main thread

### Fixed
* Fixed bug where `onAudioSessionStarted` was called twice
* Fixed bug where `audioVideo.stop()` hung when it was called in `onAttendeesLeft`
* Fixed main thread freezing issue caused by calling `audioVideo.stop()` when in reconnecting state
* Fixed bug where `onAudioSessionStopped(sessionStatus: MeetingSessionStatus)` not getting called after calling `audioVideo.stop()`

## [0.4.1] - 2020-04-23

### Added
* Added proguard rules for release build type

### Fixed
* Fix bug where external id for self is sometimes empty

### Changed
* Update `DefaultActiveSpeakerPolicy` to use consistent `cutoffThreshold` in all platforms
