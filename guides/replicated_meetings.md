# Replica Meetings Attendee Behaviors and Features

To allow extended scaling of meetings that expect significant portions of attendees be non-active participants (e.g. a live event or presentation), the Amazon Chime SDK supports the creation of Replica meetings with specialized behavior. For explanation of how to create Replica meetings via the [chime::CreateMeeting](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeeting.html) and [chime::CreateMeetingWithAttendees](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeetingWithAttendees.html) APIs, a high level overview of how Replica meetings work, and a walkthrough of how to incorporate this type of scaling in your applications meetings, see [this section of the Chime Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/media-replication.html).

In short, an attendee in a Replica meeting will receive remote media and metadata from the Primary meeting as if they were attendees of the Primary meeting (though they will not require authentication for the Primary meeting unless they would like to be promoted to an active participant, which is covered in a separate section below).

## Prerequisites

* You have read the [API overview](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/api_overview.md) and have a basic understanding of the components covered in that document.
* You have completed [Getting Started](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/getting_started.md) and have running application which uses the Amazon Chime SDK.

Note: Deploying the serverless/browser demo and receiving traffic from the demo created in this post can incur AWS charges.
* You have an understanding of the high level ideas around Replica meetings as covered in the [Chime Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/media-replication.html).

*Note:* This guide will refer to the meeting which is replicated as a Primary meeting, however there is no difference between a Primary meeting and a normal meeting. There is additionally no difference in the behavior and features of an attendee of a Primary meeting, so builders should not need to use the SDK any differently from the way they would for non-replicated meetings for those Primary meeting attendees.

## Replica meeting receive behavior

As mentioned above, an attendee in a Replica meeting will receive remote media and metadata as if they were attendees of the Primary meeting. They will not require authentication for the Primary meeting unless they would like to be promoted to a fully-interactive participant; see section below. For extended clarity, we can further break down this behavior by section. Details on send limitations are explained in the following section.

### Remote attendee events

Replica meeting attendees should subscribe to real time events as they would in a normal meeting. Post-subscription, the Replica attendee can expect following behavior:

* Attendee presence events (via [`RealtimeObserver`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/index.html)) will be received for the *Primary* meeting attendees. The callback will contain both the attendee ID and external user ID from [`chime:CreateAttendee`](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html) for the aforementioned Primary meeting attendee. *There will be **no** callback for attendees of the same Replica meeting that was joined, or any other Replica meeting.*
* Volume, signal strength, and active speaker callbacks (also via [`RealtimeObserver`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/index.html)) will correspond to *Primary* meeting attendees. *There will be **no** callback for attendees of the same Replica meeting that was joined, or any other Replica meeting.*

### Remote audio and video

A Replica attendee will receive audio played out to their chosen audio device from the Primary meeting. *This **will not** include any audio from any Replica meeting attendees.*

Similarly, remote video availability notified via [`onRemoteVideoSourceAvailable`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-available.html) and/or [`onVideoTileAdded`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-added.html) will all correspond to Primary meeting attendees (i.e. they will contain the Attendee ID and External ID from a [`chime:CreateAttendee`](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html) call for the Primary meeting). Builders should handle and respond to those callbacks, [`onRemoteVideoSourceUnavailable`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-remote-video-source-unavailable.html), and [`onVideoTileRemoved`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo.video/-video-tile-observer/on-video-tile-removed.html) in the same fashion as normal meetings.

All information related to receipt and display of video in [Configuring Remote Video Subscriptions](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/configuring_remote_video_subscriptions.md) continue to apply to Replica meeting attendees.

### Remote data messages

A replica meeting attendee which has subscribed to meeting data messages via [`DataMessageObserver`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime.datamessage/-data-message-observer/index.html) will receive data messages from Primary meeting attendees. *They **will not** receive any from other Replica meeting attendees.*

If live transcription is enabled for the Primary meeting, Replica meeting attendees will receive Transcription Events just as they would any Primary meeting data message. See the [Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/process-msgs.html) for more information on how to handle Transcription Events.

### Meeting events and ingestion

The client events and ingestion covered in [Event Ingestion](https://github.com/aws/amazon-chime-sdk-android/blob/master/guides/event_ingestion.md) (note, these are different the events such as provided by [`RealtimeObserver`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-observer/index.html)) will continue to work as before for Replica meeting attendees, all events have the meetingId attribute set to the Replica meeting ID.

### Replica meeting attendee limitations and messaging

As covered in the previous section, Replica meeting attendees will not receive media, data messages, or attendee events for *any* other Replica meeting attendee, regardless of whether they are in the same replica meeting. Additionally, Primary meeting attendees will not receive media, data messages, or attendee events for any Replica meeting attendee.

Therefore application builders should be considerate of that fact and avoid displaying UI which may indicate that other participants may be able to receive transmitted media unless there is the ability to promote as covered in section below.

### Video send availability notification

Although application builders should already be aware of when attendees are in a replica meeting, they will additionally be notified of video-related restrictions . [`onVideoSessionStarted`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-video-session-started.html) will be called again with [`MeetingSessionStatusCode.VideoAtCapacityViewOnly`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-status-code/-video-at-capacity-view-only.html) when joining a replica meeting regardless before promoted

## Low latency promotion of Replica meeting attendees into the Primary meeting

Applications may want to allow Replica attendees to authenticate against and participate in the Primary meeting by sharing audio, video and data messages without needing to incur the latency of leaving the Replica meeting and joining the Primary meeting. This may, for example, be used for Town Hall-style events where normal participants may be given the opportunity to ask their question to participants in the Primary meeting. The Amazon Chime SDK for Javascript provides a set of APIs for low latency promotions of Replica attendees: [`promoteToPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/promoteToPrimaryMeeting.html) and [`demoteFromPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/demoteFromPrimaryMeeting.html).

### Promotion of Replica attendees

Primary meeting credentials for Replica attendees which desire promotion need to be retrieved in the same way as a normal Primary meeting attendee via [`chime::CreateAttendee`](https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html) Chime APIs. See [Getting responses from your server application](https://github.com/aws/amazon-chime-sdk-js#getting-responses-from-your-server-application) for more information. Once the Primary meeting credentials have been retrieved, they can be provided to an audio video session which is already connected to a Replica meeting:

```kotlin
// We assume `replicaMeetingSession` is created using configuration created
// with Replica meeting credentials.
val meetingSession = DefaultMeetingSession(configuration, logger, applicationContext, eglCoreFactory)
// Can then use `replicaMeetingSession` as non-participating session.

// When promotion is desired...

// Response from Primary's CreateMeeting API call
val meetingResponse = getPrimaryMeeting()
// Response from CreateAttendee API call against Primary
val attendeeResponse = createPrimaryMeetingAttendee()
val configuration = MeetingSessionConfiguration(meetingResponse, meetingResponse, ::dummyUrlRewriter)
// This could also be created solely through the `CreateAttendee` response
val credentials = configuration.credentials
meetingSession.audioVideo.promoteToPrimaryMeeting(credentials, self)
```

[`promoteToPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/promoteToPrimaryMeeting.html) takes a [`PrimaryMeetingPromotionObserver`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-primary-meeting-promotion-observer/index.html) which will have [`onPrimaryMeetingPromotion`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-primary-meeting-promotion-observer/on-primary-meeting-promotion.html) called with one of a few statuses covered in documentation. If the status code is [`MeetingSessionStatusCode.OK`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-status-code/-o-k.html), the connection has successfully authenticated as a Primary meeting attendee and the attendee experience should now be as if you had joined the Primary meeting directly. This means:

* Audio captured and transmitted will now be shared with the Primary meeting and all Replica meetings. Attendee presence, volume, and signal strength callbacks for the promoted attendee on all Primary and Replica meeting attendees will be labeled with the attendee associated with the credentials provided to [`promoteToPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/promoteToPrimaryMeeting.html).
* If there is video source capacity in the Primary meeting, [`onVideoSessionStarted`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-observer/on-video-session-started.html) will be called again without [`MeetingSessionStatusCode.VideoAtCapacityViewOnly`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-status-code/-video-at-capacity-view-only.html). Video can then be enabled as normally done in the conference. In the same way as audio, this video will be associated with the Primary meeting attendee.
* Data messages sent via [`realtimeSendDataMessage`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.realtime/-realtime-controller-facade/realtime-send-data-message.html) will be forwarded to all Primary and Replica meeting attendees. They will also be associated with the Primary meeting attendee.

Builders should be considerate of these changes by showing UI related to video enable/disable, audio mute/unmute, etc. on promoted attendees.

### Demotion of promoted Replica attendees

A promoted attendee can be considered as authenticated in two meetings, as two attendees, at the same time. However with regards to their connection to the Amazon Chime SDK servers, their 'root' connection is as a Replica attendee, and thus there are three ways for the attendee to 'revert' to a view-only, Replica meeting attendee with behavior explained in earlier sections. (i.e. demotion will lead to audio, video, and data messages ceasing to be forwarded from the client). All demotions will result in a call to [`onPrimaryMeetingDemotion`]([`onPrimaryMeetingPromotion`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-primary-meeting-promotion-observer/on-primary-meeting-demotion.html))  with a corresponding status code.

* The client itself can trigger the demotion via [`demoteFromPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/start.html).
* The demotion can be forced by call to [`chime::DeleteAttendee`](https://docs.aws.amazon.com/chime/latest/APIReference/API_DeleteAttendee.html) on the Chime API.
* Any disconnection will trigger an automatic demotion to avoid unexpected or unwanted promotion state on reconnection. If the attendee still needs to be an interactive participant in the Primary meeting, [`promoteToPrimaryMeeting`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.audiovideo/-audio-video-controller-facade/promoteToPrimaryMeeting.html) should be called again with the same credentials.
