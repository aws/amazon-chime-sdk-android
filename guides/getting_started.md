# Getting Started

## Prerequisites

* You have read this [blog post](https://aws.amazon.com/blogs/business-productivity/building-a-meeting-application-using-the-amazon-chime-sdk/) to understand the basic architecture of Amazon Chime SDK and have deployed a serverless/browser demo meeting application.
* You have a basic to intermediate understanding of [Kotlin](https://kotlinlang.org/) and Android development.
* You have installed [Android Studio](https://developer.android.com/studio) and have an Android application project.

Note: A physical Android device is recommended for a better testing experience.

## Configure your application

To declare the Amazon Chime SDK as a dependency, you must complete the following steps.

1. Download [amazon-chime-sdk-media.tar.gz](https://amazon-chime-sdk.s3.amazonaws.com/android/amazon-chime-sdk-media/latest/amazon-chime-sdk-media.tar.gz) and [amazon-chime-sdk.tar.gz](https://amazon-chime-sdk.s3.amazonaws.com/android/amazon-chime-sdk/latest/amazon-chime-sdk.tar.gz)
2. Unzip the files and copy `amazon-chime-sdk-media.aar` and `amazon-chime-sdk.aar` into your application’s `libs` directory.
3. Open your project’s `build.gradle` and add the following under `repositories` in `allprojects`:
```
allprojects {
   repositories {
      jcenter()
      flatDir {
        dirs 'libs'
      }
   }
}
```
4. Add the following under `dependencies` section:
```
implementation(name: 'amazon-chime-sdk', ext: 'aar')
implementation(name: 'amazon-chime-sdk-media', ext: 'aar')
```
5. Use Java 8 features by adding the following under the `android` section.
```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```
6. `MODIFY_AUDIO_SETTINGS`, `RECORD_AUDIO`, and `CAMERA` permissions are already added to the manifest by the Amazon Chime SDK. Your activity should also request the appropriate permissions.
```
private val PERMISSION_REQUEST_CODE = 1
private val PERMISSIONS = arrayOf(
    Manifest.permission.MODIFY_AUDIO_SETTINGS,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA)

ActivityCompat.requestPermissions(applicationContext, PERMISSIONS, PERMISSION_REQUEST_CODE)
```
You are now ready to integrate with the Amazon Chime SDK for Android. Next we will walk you through the key APIs in order to have a basic audio, video and screen share viewing experience. You can refer to the [API overview](guides/api_overview.md) or the [API document](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/) for additional details.

## Create a meeting session

To start a meeting, you must complete the following steps to create a meeting session.

1. Make a POST request to `meetingUrl` to create a meeting and an attendee. The `meetingUrl` is the URL of the serverless demo meeting application you deployed (see Prerequisites section). Don’t forget to escape the inputs appropriately as shown in the following code.
Note: use https://xxxxx.xxxxx.xxx.com/Prod/

```
val attendeeName = java.net.URLEncoder.encode(attendee, "utf-8");
val region = java.net.URLEncoder.encode("us-east-1", "utf-8");
val title = java.net.URLEncoder.encode(meetingId, "utf-8");
val url = "${meetingUrl}join?title=$title&name=$attendeeName&region=$region";
```
2. Use the `response` from the previous request to construct a `MeetingSessionConfiguration`. You can convert the JSON response to the pre-defined `CreateMeetingResponse` and `CreateAttendeeResponse` types in a number of ways. In the following example we use [Gson](https://github.com/google/gson).
```
// Data stucture that maps to the HTTP response.
data class JoinMeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo)

data class MeetingInfo(
    @SerializedName("Meeting") val meetingResponse: MeetingResponse,
    @SerializedName("Attendee") val attendeeResponse: AttendeeResponse)

data class MeetingResponse(
    @SerializedName("Meeting") val meeting: Meeting)

data class AttendeeResponse(
    @SerializedName("Attendee") val attendee: Attendee)

// Deserialize the response to object.
val joinMeetingResponse = Gson().fromJson(
    response.toString(),
    JoinMeetingResponse::class.java
)

// Construct configuration using the meeting response.
val configuration = MeetingSessionConfiguration(
    CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
    CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee)
)

// Create a default meeting seesion.
val meetingSession = DefaultMeetingSession(configuration, ConsoleLogger(), applicationContext)
```

## Access AudioVideoFacade

Now that we have the meeting session, we can access the `AudioVideoFacade` instance and use it to control the audio and video experience.
```
val audioVideo = meetingSession.audioVideo

// Start audio and video clients.
audioVideo.start()
```
Your application now starts sending and receiving audio streams. You can turn local audio on and off by calling the mute and unmute methods on the facade.
```
// Mute local audio input.
audioVideo.realtimeLocalMute()

// Unmute local audio input.
audioVideo.realtimeLocalUnmute()
```
The video does not start automatically. Call the following methods to start sending local video and to start receiving remote video.
```
// Start receiving remote video.
audioVideo.startRemoteVideo()

// Start sending local video.
audioVideo.startLocalVideo()

// Switch camera for local video between front and back.
audioVideo.switchCamera()
```

## Render a video tile

To render a video tile (both local and remote), you must define a `VideoRenderView` in the layout resource file where you want to display the video tile.
```
<com.amazon.chime.sdk.media.mediacontroller.video.DefaultVideoRenderView
    android:id="@+id/video_surface"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
By implementing `onVideoTileAdded` and `onVideoTileRemoved` on the `VideoTileObserver`, you can track the currently active video tiles. The video track can come from either camera or screen share.
```
// Register the observer.
audioVideo.addVideoTileObserver(observer)

override fun onVideoTileAdded(tileState: VideoTileState) {
    logger.info(
        TAG,
        "Video tile added, titleId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}, isContent ${tileState.isContent}")

    showVideoTile(tileState)
}

override fun onVideoTileRemoved(tileState: VideoTileState) {
    logger.info(
        TAG,
        "Video tile removed, titleId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}")

    // Unbind the video tile to release the resource
    audioVideo.unbindVideoView(tileId)
}

// It could be remote or local video.
fun showVideoTile(tileState: VideoTileState) {
    // Render the DefaultVideoRenderView

    //Bind the video tile to the DefaultVideoRenderView
    audioVideo.bindVideoView(view.video_surface, tileState.tileId)
}
```

## Test

After building and running your Android application, you can verify the end-to-end behavior. Test it by joining the same meeting from your Android device and a browser (using the demo application you set up in the prerequisites).

## Cleanup

If you no longer want to keep the demo active in your AWS account and wish to avoid incurring AWS charges, the demo resources can be removed by deleting the two [AWS CloudFormation](https://aws.amazon.com/cloudformation/) stacks created in the prerequisites. These stacks can be found in the [AWS CloudFormation console](https://console.aws.amazon.com/cloudformation/home).
