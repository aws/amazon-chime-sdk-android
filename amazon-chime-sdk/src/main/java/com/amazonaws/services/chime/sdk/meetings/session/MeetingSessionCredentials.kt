/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [MeetingSessionCredentials] includes the credentials used to authenticate
 * the attendee on the meeting
 */
data class MeetingSessionCredentials(
    val attendeeId: String,
    val joinToken: String
)
