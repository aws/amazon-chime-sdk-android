# Content Share

Builders using the Amazon Chime SDK for Android can share a second video stream such as screen capture in a meeting without disrupting their applications existing audio/video stream.

## Prerequisites

* You have read the [API overview](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md) and have a basic understanding of the components covered in that document.
* You have completed [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md) and have running application which uses the Amazon Chime SDK.
* You have read the [Custom Video Sources, Processors, and Sinks](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md) and have a basic understanding of APIs such as [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html).

## Sharing content with remote participants

Content share APIs are accessible from [AudioVideoFacade](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-facade.html). Builders will need to create a [ContentShareSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-source/index.html) which currently contains just the [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html) desired to share in the meeting. A bundled screen capture source using [MediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjection) is provided in [DefaultScreenCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-screen-capture-source/index.html), but developers can also share any video source which implements [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html).

### Constructing the provided screen capture source implementation

In [DefaultScreenCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-screen-capture-source/index.html), the video frames are provided via the [MediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjection) APIs, so an application will need to complete the following prerequisites before the implementation is ready for use:

1. *(Android Q and above only)* Starting in Android Q, a foreground service is required before acquiring a `MediaProjection` through [MediaProjectionManger.getMediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjectionManager). Applications will need to create a Service, defined in your `AndroidManifest.xml` and start it as foreground before starting the screen capture source.  This will look like the following:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<application>
    <service
        android:name=".ScreenCaptureService"
        android:foregroundServiceType="mediaProjection" />
</application>
```

2. To construct a [DefaultScreenCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-screen-capture-source/index.html), request screen capture permissions from the user and use the result in the constructor parameters:

```kotlin
// Call this function when screen capture is desired
fun requestScreenCapturePermission() {
    mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    // Show prompt for screen capture permission
    startActivityForResult(
        mediaProjectionManager.createScreenCaptureIntent(),
        SCREEN_CAPTURE_REQUEST_CODE
    )
}

// Handle the result of permission prompt activity started
// in requestScreenCapturePermission
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // ...
    if (SCREEN_CAPTURE_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
        // (Android Q and above) Start the service created in step 1
        startService(Intent(this, ScreenCaptureService::class.java))
 
        // Initialize a DefaultScreenCaptureSource instance using given result
        val screenCaptureSource = DefaultScreenCaptureSource(
            this,
            logger,
            // Use the same EglCoreFactory instance as passed into DefaultMeetingSession
            DefaultSurfaceTextureCaptureSourceFactory(
                logger,
                eglCoreFactory),
            resultCode,
            data
        )
        screenCaptureSource.start()
    }
    // ... Complete any other initialization
}
```

See [Custom Video Sources, Processors, and Sinks](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md) for more information on the usage of [EglCoreFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl/-egl-core-factory/index.html). The capture source will not work if the factory is not shared between the capture and the meeting session due to use of GPU based video frames.

### Using a custom video source

Refer to the [Implementing a custom video source and transmitting](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md#implementing-a-custom-video-source-and-transmitting) section of the custom video guide to build a video source to share.  Content share supports sharing any source which implements [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html).

### Passing a video source into the facade
Once the video source is ready, wrap it with [ContentShareSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-source/index.html) to share the video.

```kotlin
// Construct the content share source
val contentShareSource = ContentShareSource()
contentShareSource.videoSource = screenCaptureSource // Or a custom source

// Start sharing the content share source to remote participants
audioVideo.startContentShare(contentShareSource)

// ...

// Stop sharing the source
audioVideo.stopContentShare()
```

Note that the content share APIs do not manage the source and only provide a sink to transmit captured frames to remote participants, builders will be responsible to take care of its lifecycle including stopping and releasing the capture sources internal resources.

### Receiving content share events

Applications can receive content share events by implementing methods from [ContentShareObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-observer/index.html) and subscribe with [addContentShareObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare/-content-share-controller/add-content-share-observer.html).


## Viewing the content

Content shares are treated as regular audio-video attendees. The attendee ID of a content share is the same as the original attendee, but with a suffix of **#content**. Applications using the Amazon Chime SDK receive real-time attendee presence and video tile updates callbacks for content attendee using the exact same mechanisms as normal video.

To view the content share:
1. Create an observer of [VideoTileObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/index.html) that implements [onVideoTileAdded](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-added.html) to receive callbacks when the video tile is added.
1. Subscribe the observer with [addVideoTileObserver](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/add-video-tile-observer.html) via audio video facade.
1. In the `onVideoTileAdded`, bind the video tile to a [VideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-render-view.html).

```kotlin
override fun onVideoTileAdded(tileState: VideoTileState) {
    // ...
    if (tileState.isContent) {
         audioVideo.bindVideoView(view.video_surface, tileState.tileId)
    }
}
```

Builders can use the [VideoTileState.isContent](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-state/is-content.html) to check if the video tile is a content share, and any add special logic you need to handle the content share.

You can also use the [DefaultModality](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils/-default-modality/index.html) class to determine that an attendee ID is a content share:

```kotlin
if (DefaultModality(attendeeId).hasModality(ModalityType.Content)) {
  // ...special handling for content share...
}

```
