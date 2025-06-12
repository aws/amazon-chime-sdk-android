# Event Ingestion

We send the [Amazon Chime SDK meeting events](meeting_events.md) to the Amazon Chime backend to analyze meeting health trends or identify common failures. This helps us to improve your meeting experience.

## Enabled by default

Event ingestion is enabled by default when using `DefaultMeetingSession`, provided that the [`ingestionURL`](https://aws.github.io/amazon-chime-sdk-android/amazon-chime-sdk/com.amazonaws.services.chime.sdk.meetings.session/-meeting-session-u-r-ls/ingestion-u-r-l.html) is properly set in the `MeetingSessionURLs`.

This URL is supplied by the [CreateMeeting API](https://docs.aws.amazon.com/chime-sdk/latest/APIReference/API_meeting-chime_CreateMeeting.html) via the `MediaPlacement.EventIngestionUrl` field. Applications that intend to use event ingestion must ensure that this field is returned and correctly passed into the meeting session configuration.

## Sensitive attributes
The Amazon Chime SDK for Android will not send below sensitive attributes to the Amazon Chime backend.
|Attribute|Description
|--|--
|`externalMeetingId`|The Amazon Chime SDK external meeting ID.
|`externalUserId`|The Amazon Chime SDK external user ID that can indicate an identity managed by your application.

## Opt out of Event Ingestion

To opt out of event ingestion, provide `NoopEventReporterFactory` to `DefaultMeetingSession` while creating the
meeting session.

See following example code:
```kotlin
    DefaultMeetingSession(
        meetingSessionConfiguration,
        logger,
        applicationContext,
        DefaultEglCoreFactory(),
        NoopEventReporterFactory()
    )
```