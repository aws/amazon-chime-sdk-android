/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [MeetingSessionConfiguration] includes information needed to start the meeting session such as
 * attendee credentials and URLs for audio and video
 *
 * Constructs a MeetingSessionConfiguration with a chime:[CreateMeetingResponse] and
 * chime:[CreateAttendeeResponse] response.
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
