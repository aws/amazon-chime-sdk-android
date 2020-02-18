package com.amazon.chime.sdkdemo.data

import com.amazon.chime.sdk.session.Attendee
import com.amazon.chime.sdk.session.Meeting
import com.google.gson.annotations.SerializedName

data class MeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo
)

data class MeetingInfo(
    @SerializedName("Title") val title: String,
    @SerializedName("Meeting") val meeting: Meeting,
    @SerializedName("Attendee") val attendee: Attendee
)
