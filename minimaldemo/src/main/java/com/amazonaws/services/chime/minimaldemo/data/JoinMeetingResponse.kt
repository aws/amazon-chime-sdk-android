/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo.data

import com.amazonaws.services.chime.sdk.meetings.session.Attendee
import com.amazonaws.services.chime.sdk.meetings.session.Meeting
import com.google.gson.annotations.SerializedName

data class JoinMeetingResponse(
    @SerializedName("JoinInfo") val joinInfo: MeetingInfo
)

data class MeetingInfo(
    @SerializedName("Meeting") val meetingResponse: Meeting,
    @SerializedName("Attendee") val attendeeResponse: Attendee
)
