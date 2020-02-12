package com.amazon.chime.sdkdemo.data

import com.google.gson.annotations.SerializedName

data class MeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo
)

data class MeetingInfo(
    @SerializedName("Title") val title: String,
    @SerializedName("Meeting") val meeting: Meeting,
    @SerializedName("Attendee") val attendee: Attendee
)

data class Attendee(
    @SerializedName("AttendeeId") val attendeeId: String,
    @SerializedName("JoinToken") val joinToken: String
)

data class Meeting(
    @SerializedName("MeetingId") val meetingId: String,
    @SerializedName("MediaPlacement") val mediaPlacement: MediaPlacement
)

data class MediaPlacement(
    @SerializedName("AudioHostUrl") val audioHostUrl: String,
    @SerializedName("TurnControlUrl") val turnControlURL: String,
    @SerializedName("SignalingUrl") val signalingURL: String
)
