# Custom Video Sources, Processors, and Sinks

Builders using the Amazon Chime SDK for video can produce, modify, and consume raw video frames transmitted or received during the call. You can allow the facade to manage its own camera capture source, provide your own custom source, or use a provided SDK capture source as the first step in a video processing pipeline which modifies frames before transmission. This guide will give an introduction and overview of the APIs involved with custom video sources.

## Prerequisites

* You have read the [API overview](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md) and have a basic understanding of the components covered in that document.
* You have completed [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md) and have running application which uses the Amazon Chime SDK.

Note: A physical Android device is recommended for a better testing experience.

## Using the provided camera capture implementation as a custom source to access additional functionality

While the Amazon Chime SDK internally uses a implementation of camera capture, the same capturer can be created, maintained, and used externally before being passed in for transmission to remote participants using the [AudioVideoFacade](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-facade.html). This grants access to the following features:

* Explicit camera device and format selection.
* Configuration, starting, stopping, and video renderering before joining the call.
* Torch/flashlight control.

The camera capture implementation is found in [DefaultCameraCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/index.html). To use the capturer in tandem with the facade, system graphics state (see official [Android OpenGL ES documentation](https://source.android.com/devices/graphics/arch-egl-opengl) for more information) must be shared between the implementation and the [AudioVideoFacade](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-facade.html). This state can be instantiated and shared through usage of a [DefaultEglCoreFactory](amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl/-default-egl-core/index.html). To create and use the camera capture source, complete the following steps:

1. Create a [DefaultEglCoreFactory](amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl/-default-egl-core/index.html) to share between the capturer and facade.

```
    val eglCoreFactory = DefaultEglCoreFactory()
```

2. Create a [DefaultCameraCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/index.html). This requires a [SurfaceTextureCaptureSourceFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-surface-texture-capture-source-factory/index.html) as a dependency. Both require a [Logger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-logger/index.html) and the camera capture source requires an application context to reach system camera APIs.

```
    val surfaceTextureCaptureSourceFactory = DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
    val cameraCaptureSource = DefaultCameraCaptureSource(applicationContext, logger, surfaceTextureCaptureSourceFactory)
```

3. Call [DefaultCameraCaptureSource.start](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/start.html) and [DefaultCameraCaptureSource.stop](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/stop.html) to start and stop the capture respectively. Note that if no [VideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/index.html) has been attached (see later sections) that captured frames will be immediately dropped.

```
    // Start the capture
    cameraCaptureSource.start()

    // Stop the capture when complete
    cameraCaptureSource.stop()
```

4. To set the capture device, use [DefaultCameraCaptureSource.switchCamera](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/switch-camera.html) or set [DefaultCameraCaptureSource.device](amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/device.html). You can get a list of usable devices by calling [MediaDevice.listVideoDevices](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-media-device/list-video-devices.html). To set the format, set [DefaultCameraCaptureSource.format](https://aws.github.ioamazon-chime-sdk-android//amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/format.html). You can get a list of usable formats by calling [MediaDevice.listSupportedVideoCaptureFormats](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-media-device/list-supported-video-capture-formats.html) with a specific [MediaDevice](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.device/-media-device/index.html). These can be set before or after capture has been started, and before or during call.

```
    // Switch the camera
    cameraCaptureSource.switchCamera()

    // Get the current device and format
    val currentDevice = cameraCaptureSource.device
    val currentFormat = cameraCaptureSource.format

    // Pick a new device explicitly (requires application context)
    val desiredDeviceType = MediaDeviceType.VIDEO_BACK_CAMERA
    val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val newDevice = MediaDevice.listVideoDevices(cameraManager).firstOrNull { it.type == desiredDeviceType } ?: return
    cameraCaptureSource.device = newDevice

    // Pick a new format explicitly
    val newFormat = MediaDevice.listSupportedVideoCaptureFormats(cameraManager, newDevice)
        .firstOrNull { it.height <= 800 } ?: return
    cameraCaptureSource.format = newFormat
```

5. To turn on the flashlight on the current camera, set [DefaultCameraCaptureSource.torchEnabled](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/torch-enabled.html). This can be set before or after capture has been started, and before or during call.

```
    // Turn on the torch
    cameraCaptureSource.torchEnabled = true

    // Turn off the torch
    cameraCaptureSource.torchEnabled = false
```

6. To render local camera feeds before joining the call, use [VideoSource.addVideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/add-video-sink.html) with a provided [VideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/index.html) (e.g. a [DefaultVideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-default-video-render-view/index.html) created as described in [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md#render-a-video-tile)).
```
    // If using outside of call (e.g. in a device selection screen), builders must initialize the render view explicitly
    view.video_surface.init(eglCoreFactory)

    // Add the render view as a sink to camera capture source
    cameraCaptureSource.addSink(view.video_surface)

    // If not reused in call (i.e. wired up to tile in `onVideoTileAdded`) remember to explicitly release the render view
    view.video_surface.release()
```

To use the capture source in a call, do the following:

1. When creating the [DefaultMeetingSession](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-default-meeting-session/index.html) (this can be done before or after the camera source is created), pass in the created factory to the (optional) last parameter.

```
    val meetingSession = DefaultMeetingSession(configuration, logger, applicationContext, eglCoreFactory)
```

2. When enabling local video, call [AudioVideoControllerFacade.startLocalVideo](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start-local-video.html) with the camera capture source as the parameter. Ensure that the capture source is started before calling `startLocalVideo` to start transmitting frames.

```
    // Start the camera capture source is started if not already
    cameraCaptureSource.start()
    audioVideo.startLocalVideo(cameraCaptureSource)
```

## Implementing a custom video source and transmitting

If builders wish to implement their own video sources (e.g. a camera capture implementation with different configuration, or a raw data source), they can do so by implementing the [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html) interface, and then producing [VideoFrame](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-frame/index.html) objects containing the raw buffers in some compatible format, similar to the following snippet. See [DefaultCameraCaptureSource code](https://github.com/aws/amazon-chime-sdk-android/blob/master/amazon-chime-sdk/src/main/java/com/amazonaws/services/chime/sdk/meetings/audiovideo/video/capture/DefaultCameraCaptureSource.kt) for a working implementation using the Camera2 API.

The following snippet contains boilerplate for maintaining a list of sinks that have been added to the source; this allows all sources to be forked to multiple targets (e.g. transmission and local rendering). See [VideoContentHint](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-content-hint/index.html) for more information on the effects of that paramater to the downstream encoder.

```
class MyVideoSource: VideoSource {
    // Do not indicate any hint to downstream encoder
    override val contentHint = VideoContentHint.None

    // Downstream video sinks
    private val sinks = mutableSetOf<VideoSink>()

    fun startProducingFrames() {
        while (true) {
            // Create buffer (e.g. `VideoFrameTextureBuffer`) from underlying source and obtain timestamp
            // ...

            // Create frame
            val frame: VideoFrame = VideoFrame(timestamp, buffer, VideoRotation.Rotation0)

            // Forward the frame to downstream sinks
            sinks.forEach { it.onVideoFrameReceived(frame) }

            // Since most video frame buffers contain manually allocated resources, we must
            // release the frame when complete
            frame.release()
        }
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }
}
```

When enabling local video, call [AudioVideoControllerFacade.startLocalVideo](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start-local-video.html) with the custom source as the parameter. Ensure that the capture source is started before calling `startLocalVideo` to start transmitting frames.

```
    // Create and start the processor
    val myVideoSource = MyVideoSource()
    myVideoSource.startProducingFrames()

    // Begin transmission of frames
    audioVideo.startLocalVideo(cameraCaptureSource)
```

## Implementing a custom video processing step for local source

By combining the [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html) and [VideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/index.html) APIs, builders can easily create a video processing step to their applications. Incoming frames can be processed, and then fanned out to downstream sinks like in the following snippet. Note that if frames are passed onto seperate threads, builders must call [VideoFrame.retain](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-frame/retain.html) to avoid the resources being dropped before the seperate thread accesses them. See example processors in [Demo code](https://github.com/aws/amazon-chime-sdk-android/blob/development/app/src/main/java/com/amazonaws/services/chime/sdkdemo/utils/GpuVideoProcessor.kt) for complete, documented implementations.

```
class MyVideoProcessor: VideoSource, VideoSink {
    // Note: Builders may want to make this mirror intended upstream source
    // or make it a constructor parameter
    override val contentHint = VideoContentHint.None

    // Downstream video sinks
    private val sinks = mutableSetOf<VideoSink>()

    override fun onVideoFrameReceived(frame: VideoFrame) {
        // Modify frame buffer ...

        val processedFrame: VideoFrame = VideoFrame(frame.timestamp, someModifiedFrame, VideoRotation.Rotation0)

        // Forward the frame to downstream sinks
        sinks.forEach { it.onVideoFrameReceived(processedFrame) }

        // Release the created frame
        processedFrame.release()
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }
}
```

To use a video frame processor, builders must use a video source external to the facade (e.g. [DefaultCameraCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/index.html)). Wire up the source to the processing step using [VideoSource.addVideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/add-video-sink.html). When enabling local video, call [AudioVideoControllerFacade.startLocalVideo](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start-local-video.html) with the processor (i.e. the end of the pipeline) as the parameter. Ensure that the capture source is started to start transmitting frames.

```
    val myVideoProcessor = MyVideoProcessor()
    // Add processor as sink to camera capture source
    cameraCaptureSource.addVideoSink(myVideoProcessor)

    // Use video processor as source to transmitted video
    audioVideo.startLocalVideo(myVideoProcessor)
```

## Implementing a custom video sink for remote sources

Though most builders will simply use [DefaultVideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-default-video-render-view/index.html), they can also implement their own [VideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/index.html)/[VideoRenderView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-render-view.html) (currently `VideoRenderView` is just an alias for `VideoSink`), some may want full control over the frames for remote video processing, storage, or other applications. To do so implement the [VideoSink](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/index.html) interface like in the following snippet.

```
class MyVideoSink: VideoSink {
    override fun onVideoFrameReceived(frame: VideoFrame) {
        // Store, render, or upload frame
    }
}
```

When a tile is added, simply pass in the custom sink to [VideoTileControllerFacade.bindVideoView](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-controller-facade/bind-video-view.html) and it will begin to receive remote frames:

```
override fun onVideoTileAdded(tileState: VideoTileState) {
    // Create a new custom sink
    val myVideoSink = MyVideoSink()

    // Bind it to the tile ID
    audioVideo.bindVideoView(myVideoSink, tileState.tileId)
}
```
