# Integrating background filters into your Amazon Chime SDK for Android application

## What is a Background Blur and Replacement?
The background blur and replacement APIs allow builders to apply background filter (blur or replacement) on frames received from a video source. The filter processors uses a TensorFlow Lite (TFLite) machine learning model to segment the foreground of a frame and then apply on top of the blurred background or a replacement image. Follow this guide for more information on how to use `BackgroundBlurVideoFrameProcessor` and `BackgroundReplacementVideoFrameProcessor`.

Background blur and replacement are integrated in the `AmazonChimeSDKDemo` app. To try it out, follow these steps:
1. Run the `AmazonChimeSDKDemo` on your device.
2. Join a meeting.
3. Enable video.
4. Click on the `video` tab.
5. Click on the icon with three dots under your local video tile.
6. Click `Turn on background blur` or `Turn on background replacement` filter from the menu.

# Getting Started
	 
## Prerequisites
	 
* Have `amazon-chime-sdk-machine-learning` library imported. Follow [README](https://github.com/aws/amazon-chime-sdk-android#manually-download-sdk-binaries) for more information on how to import these dependencies.
* You have read the [API overview](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md) and have a basic understanding of the components covered in that document.
* You have completed [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md) and have running application which uses the Amazon Chime SDK.
* You have read the [Custom Video Sources, Processors, and Sinks](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/custom_video.md) and have a basic understanding of APIs such as [VideoSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/index.html).

## Overview
`BackgroundBlurVideoFrameProcessor` and `BackgroundReplacementVideoFrameProcessor` uses [`VideoSource`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-source/) and [`VideoSink`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-sink/) APIs to consume and modify frames which are then fanned out to downstream sinks. To use the processors, builders must wire up the processor to a video source external (e.g. `DefaultCameraCaptureSource`) using `VideoSource.addVideoSink(sink)`. Then enable the local video with background blur or replacement processor as the source using `AudioVideoFacade.startLocalVideo(source)`.
	 
### Implementing background blur in your application.

1. Create a [DefaultCameraCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/index.html). This requires a [SurfaceTextureCaptureSourceFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-surface-texture-capture-source-factory/index.html) as a dependency. Both require a [Logger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-logger/index.html) and the camera capture source requires an application context to reach system camera APIs.

```kotlin
val surfaceTextureCaptureSourceFactory = DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
val cameraCaptureSource = DefaultCameraCaptureSource(applicationContext, logger, surfaceTextureCaptureSourceFactory)
```
2. Create a `BackgroundBlurVideoFrameProcessor`. Its constructor takes four parameters:

```kotlin
logger: Logger,
eglCoreFactory: EglCoreFactory,
context: Context,
configurations: BackgroundBlurConfiguration
```
- Logger to log any warnings or errors.
- A [DefaultEglCoreFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl/-default-egl-core-factory/index.html) to share between the capturer and facade.

```kotlin
val eglCoreFactory = DefaultEglCoreFactory()
```
- Application Context
- BackgroundBlurConfiguration - blurStrength specifies blurValue that corresponds to blur radius used in gaussian blur. The higher the value, more blurrier the image will be. The processor will default to `7.0f` if not provided by the builder.

```kotlin
val backgroundBlurVideoFrameProcessor = BackgroundBlurVideoFrameProcessor(
    ConsoleLogger(LogLevel.DEBUG),
    eglCoreFactory,
    applicationContext,
    BackgroundBlurConfiguration(12.5f)
)

```
3. Add the background blur processor as sink to the video source (e.g. `DefaultCameraCaptureSource`)

```kotlin
cameraCaptureSource.addVideoSink(sink: backgroundBlurVideoFrameProcessor)
```
4. Use background blur processor as source
   
```kotlin
audioVideo.startLocalVideo(backgroundBlurVideoFrameProcessor)
```

`BackgroundBlurVideoFrameProcessor` will receive the frames and apply the foreground on top of the blurred background image which is sent to the downstream sinks to render the modified frame. 
	 
## Implementing background replacement in your application.

1. Create a [DefaultCameraCaptureSource](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-camera-capture-source/index.html). This requires a [SurfaceTextureCaptureSourceFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture/-default-surface-texture-capture-source-factory/index.html) as a dependency. Both require a [Logger](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.utils.logger/-logger/index.html) and the camera capture source requires an application context to reach system camera APIs.

```kotlin
val surfaceTextureCaptureSourceFactory = DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
val cameraCaptureSource = DefaultCameraCaptureSource(applicationContext, logger, surfaceTextureCaptureSourceFactory)
```
2. Create a `BackgroundReplacementVideoFrameProcessor`. Its constructor takes four parameters:

```kotlin
logger: Logger,
eglCoreFactory: EglCoreFactory,
context: Context,
configurations: BackgroundReplacementConfiguration
```
- Logger to log any warnings or errors.
- A [DefaultEglCoreFactory](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl/-default-egl-core-factory/index.html) to share between the capturer and facade.

```kotlin
val eglCoreFactory = DefaultEglCoreFactory()
```
- Application Context
- BackgroundReplacementConfiguration - an image Bitmap to replace video background with. Defaults to shaded blue colored image. See `BackgroundReplacementConfiguration` for more information. The below code provides an example on how to load an image Bitmap from a URL string.

```kotlin
fun loadBackgroundReplacementVideoFrameProcessorWithImage(processor: BackgroundReplacementVideoFrameProcessor, imageUrl: String) {
    GlobalScope.launch(Dispatchers.IO) {
        val image: Bitmap = try {
            val url = URL(imageUrl)
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (exception: Exception) {
            throw exception
        }
        processor.configurations = BackgroundReplacementConfiguration(image)
    }
}

// Use the default until the image is loaded asynchronously.
val backgroundReplacementVideoFrameProcessor = BackgroundReplacementVideoFrameProcessor(
    ConsoleLogger(LogLevel.DEBUG),
    eglCoreFactory,
    applicationContext,
    BackgroundReplacementConfiguration()
)
loadBackgroundReplacementVideoFrameProcessorWithImage(backgroundReplacementVideoFrameProcessor, "https://...")
```
3. Add the background replacement processor as sink to the video source (e.g. `DefaultCameraCaptureSource`)

```kotlin
cameraCaptureSource.addVideoSink(sink: backgroundReplacementVideoFrameProcessor)
```
4. Use background replacement processor as source

```kotlin
audioVideo.startLocalVideo(backgroundReplacementVideoFrameProcessor)
```
`BackgroundReplacementVideoFrameProcessor` will receive the frames and apply the foreground on top of the replacement image which is sent to the downstream sinks to render the modified frame. 
