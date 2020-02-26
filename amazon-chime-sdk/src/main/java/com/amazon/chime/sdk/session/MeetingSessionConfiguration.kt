/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionCredentials]] includes the credentials used to authenticate
 * the attendee on the meeting
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
