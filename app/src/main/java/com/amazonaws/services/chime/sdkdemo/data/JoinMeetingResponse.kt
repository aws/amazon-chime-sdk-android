/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.data

import com.amazonaws.services.chime.sdk.meetings.session.Attendee
import com.amazonaws.services.chime.sdk.meetings.session.MediaPlacement
import com.google.gson.annotations.SerializedName

data class JoinMeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo
)

data class MeetingInfo(
    @SerializedName("Meeting") val meetingResponse: MeetingResponse,
    @SerializedName("Attendee") val attendeeResponse: AttendeeResponse,
    @SerializedName("PrimaryExternalMeetingId") val primaryExternalMeetingId: String
)

data class MeetingResponse(
    @SerializedName("Meeting") val meeting: MeetingResp
)

data class AttendeeResponse(
    @SerializedName("Attendee") val attendee: Attendee
)
data class MeetingResp(
    val ExternalMeetingId: String?,
    val MediaPlacement: MediaPlacement,
    val MediaRegion: String,
    val MeetingId: String,
    val MeetingFeatures: MeetingFeaturesResp?
)
data class MeetingFeaturesResp constructor(
    val Audio: AudioFeatures?,
    val Video: VideoFeatures?,
    val Content: VideoFeatures?,
    val Attendee: AttendeeFeatures?
)
data class AudioFeatures @JvmOverloads constructor(
    val EchoReduction: String?
)
data class VideoFeatures @JvmOverloads constructor(
    val MaxResolution: String?
)
data class AttendeeFeatures @JvmOverloads constructor(
    val MaxCount: Int?
)
