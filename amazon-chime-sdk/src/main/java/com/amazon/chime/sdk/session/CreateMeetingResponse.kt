package com.amazon.chime.sdk.session

data class CreateMeetingResponse(val meeting: Meeting)

data class Meeting(
    val meetingId: String,
    val mediaPlacement: MediaPlacement
)

data class MediaPlacement(val audioHostUrl: String)
