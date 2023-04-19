# Getting Started

## Prerequisites

* You have a basic to intermediate understanding of [Kotlin](https://kotlinlang.org/) and Android development.
* You have installed [Android Studio](https://developer.android.com/studio) and have an Android application project.

NOTE: Please make sure that you are running on ARM supported devices (real devices) or simulator with arm supported. We do not support x86 currently, so simulators with x86 will not work.

## Setup

### Install the Amazon Chime SDK 
You can install the SDK as a dependency using Gradle, or [manually download binaries](#manually-downloading-aars).

To obtain the dependencies from Maven Central, add the dependencies to your app's (module-level) `build.gradle` and add the following under dependencies:
```gradle
dependencies {
    // ...
    implementation 'software.aws.chimesdk:amazon-chime-sdk:0.18.0'
}
```

Use Java 8 features by adding the following under the `android` section.
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

### Permissions
SDK requires [MODIFY_AUDIO_SETTINGS](https://developer.android.com/reference/android/Manifest.permission#MODIFY_AUDIO_SETTINGS), [RECORD_AUDIO](https://developer.android.com/reference/android/Manifest.permission#RECORD_AUDIO) permission for audio, and [CAMERA](https://developer.android.com/reference/android/Manifest.permission#CAMERA) permission for video from the appliction to start the audio and video. Follow the Android developer [guide](https://developer.android.com/training/permissions/requesting) for requesting the rumtime permissions.

You are now ready to integrate with the Amazon Chime SDK for Android. Next we will walk you through the key APIs in order to have a basic audio, video and screen share viewing experience. You can refer to the [API overview](/api_overview.md) or the [API document](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/) for additional details.

## Create a meeting session

To start a meeting, you must complete the following steps to create a meeting session.

1. You will need to call [chime:CreateMeeting](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeeting.html) and [chime:CreateAttendee](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html). Your server application should make these API calls and securely pass the meeting and attendee responses to the client application. You will use this to configure the meeting session in the next step.

    For demo purposes, we provide a [serverless demo](https://github.com/aws/amazon-chime-sdk-js/blob/main/demos/serverless/README.md) that you can utilize to create a meeting and attendee. Note that deploying this serverless demo can incur AWS charges.

2. Use the response of `chime:CreateMeeting` and `chime:CreateAttendee` above to construct a `MeetingSessionConfiguration`. You can convert the JSON response to the SDK `CreateMeetingResponse` and `CreateAttendeeResponse` types in a number of ways. In the following example we use [Gson](https://github.com/google/gson).
    ```kotlin
    import com.amazonaws.services.chime.sdk.meetings.session.Attendee
    import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
    import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
    import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
    import com.amazonaws.services.chime.sdk.meetings.session.Meeting
    import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
    import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
    import com.google.gson.Gson
    import com.google.gson.annotations.SerializedName

    // Data stucture that maps to the serverless demo HTTP response.
    data class JoinMeetingResponse(@SerializedName("JoinInfo") val joinInfo: MeetingInfo)

    data class MeetingInfo(
        @SerializedName("Meeting") val meetingResponse: MeetingResponse,
        @SerializedName("Attendee") val attendeeResponse: AttendeeResponse
    )

    data class MeetingResponse(@SerializedName("Meeting") val meeting: Meeting)

    data class AttendeeResponse(@SerializedName("Attendee") val attendee: Attendee)

    // Deserialize the response to object.
    val joinMeetingResponse = Gson().fromJson(
        response.toString(),
        JoinMeetingResponse::class.java
    )

    // Construct configuration using the meeting response.
    val configuration = MeetingSessionConfiguration(
        createMeetingResponse = CreateMeetingResponse(
            Meeting = joinMeetingResponse.joinInfo.meetingResponse.meeting
        ),
        createAttendeeResponse = CreateAttendeeResponse(
            Attendee = joinMeetingResponse.joinInfo.attendeeResponse.attendee
        )
    )

    // Create a default meeting seesion.
    val meetingSession = DefaultMeetingSession(
        configuration = configuration, 
        logger = ConsoleLogger(),
        context = applicationContext
    )
    ```

## Access AudioVideoFacade

Now that we have the meeting session, we can access the `AudioVideoFacade` instance and use it to control the audio and video experience.
```kotlin
val audioVideo = meetingSession.audioVideo

// Start session.
audioVideo.start()
```
Your application now starts sending and receiving audio streams. You can turn local audio on and off by calling the mute and unmute methods on the facade.
```kotlin
// Mute local audio input.
audioVideo.realtimeLocalMute()

// Unmute local audio input.
audioVideo.realtimeLocalUnmute()
```
The video does not start automatically. Call the following methods to start sending local video and to start receiving remote video.
```kotlin
// Start receiving remote video.
audioVideo.startRemoteVideo()

// Start sending local video.
audioVideo.startLocalVideo()

// Switch camera for local video between front and back.
audioVideo.switchCamera()
```

## Render a video tile

To render a video tile (both local and remote), you must define a `VideoRenderView` in the layout resource file where you want to display the video tile.
```xml
<com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
    android:id="@+id/video_surface"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
By implementing `onVideoTileAdded` and `onVideoTileRemoved` on the `VideoTileObserver`, you can track the currently active video tiles. The video track can come from either camera or content share.
```kotlin
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState

val observer = object : VideoTileObserver {
    val view = // View that contains the video render view

    override fun onVideoTileAdded(tileState: VideoTileState) {
        // Bind the tile to the render view to display the video
        audioVideo.bindVideoView(view.video_surface, tileState.tileId)
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        // Unbind the tile to release the resource
        audioVideo.unbindVideoView(tileState.tileId)
    }

    // ...
}

// Register the observer.
audioVideo.addVideoTileObserver(observer)
```

## Test

After building and running your Android application, you can verify the end-to-end behavior. Test it by joining the same meeting from your Android device and a browser (using the demo application you set up in the [prerequisites](#prerequisites)).

## Cleanup

If you no longer want to keep the demo active in your AWS account and wish to avoid incurring AWS charges, the demo resources can be removed by deleting the two [AWS CloudFormation](https://aws.amazon.com/cloudformation/) stacks created in the prerequisites. These stacks can be found in the [AWS CloudFormation console](https://console.aws.amazon.com/cloudformation/home).

## More
### Manually downloading AARs 
1. Download [amazon-chime-sdk-media.tar.gz](https://amazon-chime-sdk.s3.amazonaws.com/android/amazon-chime-sdk-media/latest/amazon-chime-sdk-media.tar.gz) and [amazon-chime-sdk.tar.gz](https://amazon-chime-sdk.s3.amazonaws.com/android/amazon-chime-sdk/latest/amazon-chime-sdk.tar.gz)
1. Unzip the files and copy `amazon-chime-sdk-media.aar` and `amazon-chime-sdk.aar` into your project's `libs` directory.
1. Open your project’s `build.gradle` and add the following under `repositories` in `allprojects`:
    ```gradle
    allprojects {
        repositories {
            // ...
            flatDir {
                dirs 'libs'
            }
        }
    }
    ```
1. Add the following under `dependencies` section:
    ```gradle
    implementation(name: 'amazon-chime-sdk', ext: 'aar')
    implementation(name: 'amazon-chime-sdk-media', ext: 'aar')
    ```
