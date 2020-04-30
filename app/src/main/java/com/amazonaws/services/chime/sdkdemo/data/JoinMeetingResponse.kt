/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.data

import com.amazonaws.services.chime.sdk.meetings.session.Attendee
import com.amazonaws.services.chime.sdk.meetings.session.Meeting
import com.google.gson.annotations.SerializedName

data class JoinMeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo
)

data class MeetingInfo(
    @SerializedName("Meeting") val meetingResponse: MeetingResponse,
    @SerializedName("Attendee") val attendeeResponse: AttendeeResponse
)

data class MeetingResponse(
    @SerializedName("Meeting") val meeting: Meeting
)

data class AttendeeResponse(
    @SerializedName("Attendee") val attendee: Attendee
)
