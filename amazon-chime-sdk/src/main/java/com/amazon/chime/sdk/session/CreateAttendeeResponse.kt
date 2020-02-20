/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

// https://docs.aws.amazon.com/chime/latest/APIReference/API_CreateAttendee.html
data class CreateAttendeeResponse(val Attendee: Attendee)

data class Attendee(
    val AttendeeId: String,
    val JoinToken: String
)
