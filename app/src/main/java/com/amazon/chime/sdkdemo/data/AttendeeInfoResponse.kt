package com.amazon.chime.sdkdemo.data

import com.google.gson.annotations.SerializedName

data class AttendeeInfoResponse(
    @SerializedName("AttendeeInfo") val attendeeInfo: AttendeeInfo
)

data class AttendeeInfo(
    @SerializedName("AttendeeId") val attendeeId: String,
    @SerializedName("Name") val name: String
)
