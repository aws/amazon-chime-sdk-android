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
        createMeetingResponse.meeting.meetingId, MeetingSessionCredentials(
            createAttendeeResponse.attendee.attendeeId,
            createAttendeeResponse.attendee.joinToken
        ), MeetingSessionURLs(createMeetingResponse.meeting.mediaPlacement.audioHostUrl)
    )
}
