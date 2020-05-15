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
