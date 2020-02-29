/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionConfiguration]] includes information needed to start the meeting session such as
 * attendee credentials and URLs for audio and video
 */
data class MeetingSessionConfiguration(
    val meetingId: String,
    val credentials: MeetingSessionCredentials,
    val urls: MeetingSessionURLs
) {
    constructor(
        createMeetingResponse: CreateMeetingResponse,
        createAttendeeResponse: CreateAttendeeResponse
    ) : this(
        createMeetingResponse.Meeting.MeetingId,
        MeetingSessionCredentials(
            createAttendeeResponse.Attendee.AttendeeId,
            createAttendeeResponse.Attendee.JoinToken
        ),
        MeetingSessionURLs(
            createMeetingResponse.Meeting.MediaPlacement.AudioFallbackUrl,
            createMeetingResponse.Meeting.MediaPlacement.AudioHostUrl,
            createMeetingResponse.Meeting.MediaPlacement.TurnControlUrl,
            createMeetingResponse.Meeting.MediaPlacement.SignalingUrl
        )
    )
}
