# Meeting Events

The `onEventReceived` observer method makes it easy to collect, process, and monitor meeting events.
You can use meeting events to identify and troubleshoot the cause of device and meeting failures.

To receive meeting events, add an observer, and implement the `onEventReceived` observer method.

```kotlin
val observer = object: EventAnalyticsObserver {
    override fun onEventReceived(name, attributes) {
      // Handle a meeting event.
    }
}

meetingSession.audioVideo.addEventAnalyticsObserver(observer);
```

In the `onEventReceived` observer method, we recommend that you handle each meeting event so that 
you don't have to worry about how your event processing would scale when the later versions of Chime SDK introduce new meeting events.

For example, the code outputs error information for three failure events at the `error` log level.

```kotlin
override fun onEventReceived(name: EventName, attributes: EventAttributes) {
    when (name) {
        EventName.videoInputFailed -> 
            logger.error(TAG, "Video input failed $name ${attributes.text()}")
        EventName.meetingStartFailed -> 
            logger.error(TAG, "Meeting start failed $name ${attributes.text()}")
        EventName.meetingFailed ->
            logger.error(TAG, "Meeting failed $name ${attributes.text()}")
        else -> Unit
    }
}
```

Ensure that you are familiar with the attributes you want to use. See the following two examples.
The code logs the last 5 minutes of the meeting history attribute when a failure event occurs.
It's helpful to reduce the amount of data sent to your server application or analytics tool.

```kotlin
override fun onEventReceived(name: EventName, attributes: EventAttributes) {
    val meetingHistory = meetingSession.audioVideo.getMeetingHistory()
    val lastFiveMinutes = System.currentTimeMillis() - 300_000
    val recentMeetingHistory = meetingHistory?.filter { it.timestamp >= lastFiveMinutes }

    when (name) {
        EventName.VideoInputFailed,
        EventName.MeetingStartFailed,
        EventName.MeetingFailed ->
          logger.error("Failure $name ${attributes.text()} $recentMeetingHistory")
        else -> Unit
    }
}
```

There could be case where builders might want to use `EventAnalyticsController` to invoke events on their custom classes. One simple example is `DefaultCameraCaptureSource`. In this case, you can simply do set the `eventAnalyticsController` with `meetingSession.eventAnalyticsController`.

```kotlin
val meetingSessionConfig = /* MeetingSessionConfiguration instance */
val eglCoreFactory = /* instance of EglCoreFactory */
val surfaceTextureCaptureSourceFactory = /* instance of SurfaceTextureCaptureSourceFactory */

val cameraCaptureSource = DefaultCameraCaptureSource(
    applicationContext,
    logger,
    surfaceTextureCaptureSourceFactory
)

val meetingSession = DefaultMeetingSession(
    meetingSessionConfig,
    logger,
    applicationContext,
    eglCoreFactory
)

cameraCaptureSource.eventAnalyticsController = meetingSession.eventAnalyticsController
```

## Meeting events and attributes
Chime SDK sends these meeting events.
|Event name            |Description
|--                    |--
|`meetingStartRequested` |The meeting will start.
|`meetingStartSucceeded` |The meeting started.
|`meetingStartFailed`    |The meeting failed to start.
|`meetingEnded`          |The meeting ended.
|`meetingFailed`         |The meeting ended with one of the following failure [MeetingSessionStatusCode](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-status-code/index.html): <br><ul><li>`AudioJoinedFromAnotherDevice`</li><li>`AudioDisconnectAudio`</li><li>`AudioAuthenticationRejected`</li><li>`AudioCallAtCapacity`</li><li>`AudioCallEnded`</li><li>`AudioInternalServerError`</li><li>`AudioServiceUnavailable`</li><li>`AudioDisconnected`</li></ul>
|`videoInputFailed`      |The camera selection failed.

### Common attributes
Chime SDK stores common attributes for event to identify the event.

```kotlin
meetingSession.audioVideo.getCommonAttributes()
```

|Attribute|Description
|--|--
|`attendeeId`|The Amazon Chime SDK attendee ID.
|`deviceName`|The manufacturer and model name of the computer or mobile device. `unknown` indicates that the device name can't be found.
|`deviceManufacturer`|The manufacturer of the computer or mobile device. `unknown` indicates that the device name can't be found.
|`deviceModel`|The model name of the computer or mobile device. `unknown` indicates that the device name can't be found.
|`externalMeetingId`|The Amazon Chime SDK external meeting ID.
|`externalUserId`|The Amazon Chime SDK external user ID that can indicate an identify managed by your application.
|`meetingId`|The Amazon Chime SDK meeting ID.
|`mediaSdkVersion`|The Amazon Chime Android Media SDK version.
|`osName`|The operating system.
|`osVersion`|The version of the operating system.
|`sdkName`|The Amazon Chime SDK name, such as `amazon-chime-sdk-android`.
|`sdkVersion`|The Amazon Chime SDK version.

### Standard attributes
Chime SDK sends a meeting event with attributes. These standard attributes are available as part of every event type.

|Attribute|Description
|--|--
|`timestampMs`|The local time, in milliseconds since 00:00:00 UTC on 1 January 1970, at which an event occurred.<br><br>Unit: Milliseconds


### Meeting attributes
The following table describes attributes for a meeting.
|Attribute|Description|Included in
|--|--|--
|`maxVideoTileCount`|The maximum number of simultaneous video tiles shared during the meeting. This includes a local tile (your video), remote tiles, and content shares.<br><br>Unit: Count|`meetingStartSucceeded`, `meetingStartFailed`, `meetingEnded`, `meetingFailed`
|`meetingDurationMs`|The time that elapsed between the beginning (`AudioVideoObserver.onAudioSessionStarted`) and the end (`AudioVideoObserver.onAudioSessionStopped`) of the meeting.<br><br>Unit: Milliseconds|`meetingStartSucceeded`, `meetingStartFailed`, `meetingEnded`, `meetingFailed`
|`meetingErrorMessage`|The error message that explains why the meeting has failed.| `meetingFailed`
|`meetingStatus`|The meeting status when the meeting ended or failed. Note that this attribute indicates an enum name in [MeetingSessionStatusCode](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-status-code/index.html), such as `Left` or `MeetingEnded`.|`meetingStartFailed`, `meetingEnded`, `meetingFailed`
|`poorConnectionCount`|The number of times the significant packet loss occurred during the meeting. Per count, you receive `AudioVideoObserver.onConnectionBecamePoor`.<br><br>Unit: Count|`meetingStartSucceeded`, `meetingStartFailed`, `meetingEnded`, `meetingFailed`
|`retryCount`|The number of connection retries performed during the meeting.<br><br>Unit: Count|`meetingStartSucceeded`, `meetingStartFailed`, `meetingEnded`, `meetingFailed`

### Device attributes
The following table describes attributes for the camera.
|Attribute|Description|Included in
|--|--|--
|`videoInputError`|The error message that explains why the camera selection failed.|`videoInputFailed`

### The meeting history attribute
The meeting history attribute is a list of states. Each state object contains the state name and timestamp.

```kotlin
meetingSession.audioVideo.getMeetingHistory()
```

```
[
  {
    name: 'audioInputSelected',
    timestampMs: 1612166400000
  },
  {
    name: 'meetingStartSucceeded',
    timestampMs: 1612167400000
  },
  {
    name: 'meetingEnded',
    timestampMs: 1612167900000
  }
]
```
You can use the meeting history to track user actions and events from the creation of the `DefaultMeetingSession` object.
For example, if you started a meeting twice using the same `DefaultMeetingSession` object,
the meeting history will include two `meetingStartSucceeded`.

> Note: that meeting history can have a large number of states. Ensure that you process the meeting history before sending it to your server application or analytics tool.

The following table lists available states.
|State|Description
|--|--
|`audioInputSelected`|The microphone was selected.
|`meetingEnded`|The meeting ended.
|`meetingFailed`|The meeting ended with the failure status.
|`meetingReconnected`|The meeting reconnected.
|`meetingStartFailed`|The meeting failed to start.
|`meetingStartRequested`|The meeting will start.
|`meetingStartSucceeded`|The meeting started.
|`videoInputFailed`|The camera selection failed.
|`videoInputSelected`|The camera was selected.

## Example

This section includes sample code for the [Monitoring and troubleshooting with Amazon Chime SDK meeting events](https://aws.amazon.com/blogs/business-productivity/monitoring-and-troubleshooting-with-amazon-chime-sdk-meeting-events/) blog post.

1. Follow the [blog post](https://aws.amazon.com/blogs/business-productivity/monitoring-and-troubleshooting-with-amazon-chime-sdk-meeting-events/) to deploy the AWS CloudFormation stack. The stack provisions all the infrastructure required to search and analyze meeting events in Amazon CloudWatch.
2. To receive meeting events in your Android application, add an event analytics observer to implement the `onEventReceived` method. Add Internet permission ([android.permission.INTERNET](https://developer.android.com/reference/android/Manifest.permission#INTERNET)) to your Android manifest file to send a request.
    ```kotlin
    class MyObserver: EventAnalyticsObserver {
        private val meetingEvents = mutableListOf<MutableMap<String, Any>>()
        
        override fun onEventReceived(name: EventName, attributes: EventAttributes) {
            attributes.putAll(meetingSession.audioVideo.getCommonEventAttributes())
            val meetingHistory = meetingSession.audioVideo.getMeetingHistory()
            val lastFiveMinutes = ystem.currentTimeMillis() - 300_000
            val recentMeetingHistory = meetingHistory.filter { it.timestamp >= lastFiveMinutes }
            when (name) {
                EventName.videoInputFailed,
                EventName.meetingStartFailed,
                EventName.meetingFailed -> {
                    attributes.putAll(
                        mutableMapOf(
                            EventAttributeName.meetingHistory to recentMeetingHistory
                        )
                    )
                }
                else -> Unit
            }

            meetingEvents.add(mutableMapOf(
                "name" to name.toString(),
                "attributes" to attributes
            ))
        }
        
        meetingSession.audioVideo.addEventAnalyticsObserver(this);
    }
    ```
3. The next step uses [Gson](https://github.com/google/gson) as a dependency to convert meeting events to JSON. Add it to your Gradle build.
    ```
    dependencies {
      implementation 'com.google.code.gson:gson:2.8.6'
    }
    ```
4. When a meeting ends, upload meeting events to the endpoint that you created in Step 1. Set the endpoint to the **MeetingEventApiEndpoint** value from the **Outputs** tab of the AWS CloudFormation console
    ```kotlin
    class MyObserver: AudioVideoObserver {
        private val uiScope = CoroutineScope(Dispatchers.Main)
        override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
            uiScope.launch {
                makeRequest(gson.toJson(meetingEvents), "Event")
            }
        }
        
        // Other AudioVideo related functions are omitted for conveninence.
        
        private suspend fun makeRequest(body: String, tag: String) {
            withContext(Dispatchers.IO) {
                val serverUrl = URL(/* MeetingEventApiEndpoint from Step 1 */)
                try {
                    val response = StringBuffer()
                    with(serverUrl.openConnection() as HttpURLConnection) {
                        requestMethod = "POST"
                        doInput = true
                        doOutput = true
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use {
                            val input = body.toByteArray(Charsets.UTF_8)
                            it.write(input, 0, input.size)
                        }

                        BufferedReader(InputStreamReader(inputStream)).use {
                            var inputLine = it.readLine()
                            while (inputLine != null) {
                                response.append(inputLine)
                                inputLine = it.readLine()
                            }
                            it.close()
                        }

                        if (responseCode == 200) {
                            Log.i(tag, "Publishing log was successful")
                            meetingEvents.clear()
                            response.toString()
                        } else {
                            Log.e(tag, "Unable to publish log. Response code: $responseCode")
                            null
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(tag, "There was an exception while posting logs: $exception")
                    null
                }
            }
        }
        
        meetingSession.audioVideo.addAudioVideoObserver(this)
    }
    ```
5. Now that your applications upload meeting events to Amazon CloudWatch. Run several test meetings to collect meeting events. For an example of how to troubleshoot with meeting events, see the [blog post](https://aws.amazon.com/blogs/business-productivity/monitoring-and-troubleshooting-with-amazon-chime-sdk-meeting-events/).
