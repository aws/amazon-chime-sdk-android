package com.amazon.chime.sdk.session

/**
 * [[MeetingSessionCredentials]] includes the credentials used to authenticate
 * the attendee on the meeting
 */
data class MeetingSessionCredentials(
    val attendeeId: String,
    val joinToken: String
)
