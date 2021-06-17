# Event Ingestion

We send the [Amazon Chime SDK meeting events](meeting_events.md) to the Amazon Chime backend to analyze meeting health trends or identify common failures. This helps us to improve your meeting experience.

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