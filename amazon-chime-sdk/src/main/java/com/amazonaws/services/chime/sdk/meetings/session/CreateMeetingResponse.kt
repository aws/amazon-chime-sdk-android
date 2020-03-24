/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

// https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateMeeting.html
data class CreateMeetingResponse(val Meeting: Meeting)

data class Meeting(
    val MeetingId: String,
    val MediaPlacement: MediaPlacement
)

data class MediaPlacement(
    val AudioFallbackUrl: String,
    val AudioHostUrl: String,
    val TurnControlUrl: String,
    val SignalingUrl: String
)
