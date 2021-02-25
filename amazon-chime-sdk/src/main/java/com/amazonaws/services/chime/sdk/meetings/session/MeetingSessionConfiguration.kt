/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType

/**
 * [MeetingSessionConfiguration] includes information needed to start the meeting session such as
 * attendee credentials and URLs for audio and video
 *
 * Constructs a MeetingSessionConfiguration with a chime:[CreateMeetingResponse] and
 * chime:[CreateAttendeeResponse] response and optional custom [URLRewriter] that will
 * rewrite urls given to new urls.
 */
data class MeetingSessionConfiguration(
    val meetingId: String,
    val externalMeetingId: String?,
    val credentials: MeetingSessionCredentials,
    val urls: MeetingSessionURLs
) {
    @JvmOverloads constructor(
        createMeetingResponse: CreateMeetingResponse,
        createAttendeeResponse: CreateAttendeeResponse,
        urlRewriter: URLRewriter = ::defaultUrlRewriter
    ) : this(
        createMeetingResponse.Meeting.MeetingId,
        createMeetingResponse.Meeting.ExternalMeetingId,
        MeetingSessionCredentials(
            createAttendeeResponse.Attendee.AttendeeId,
            createAttendeeResponse.Attendee.ExternalUserId,
            createAttendeeResponse.Attendee.JoinToken
        ),
        MeetingSessionURLs(
            createMeetingResponse.Meeting.MediaPlacement.AudioFallbackUrl,
            createMeetingResponse.Meeting.MediaPlacement.AudioHostUrl,
            createMeetingResponse.Meeting.MediaPlacement.TurnControlUrl,
            createMeetingResponse.Meeting.MediaPlacement.SignalingUrl,
            urlRewriter
        )
    )

    constructor(
        meetingId: String,
        credentials: MeetingSessionCredentials,
        urls: MeetingSessionURLs
    ) : this(meetingId, null, credentials, urls)

    fun createContentShareMeetingSessionConfiguration(): MeetingSessionConfiguration {
        val contentModality: String = DefaultModality.MODALITY_SEPARATOR + ModalityType.Content.value
        return MeetingSessionConfiguration(
            meetingId,
            externalMeetingId,
            MeetingSessionCredentials(
                credentials.attendeeId + contentModality,
                credentials.externalUserId,
                credentials.joinToken + contentModality
            ),
            urls
        )
    }
}
