/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [MeetingSessionURLs] contains the URLs that will be used to reach the
 * meeting service.
 */
data class MeetingSessionURLs(
    val audioFallbackURL: String,
    val audioHostURL: String,
    val turnControlURL: String,
    val signalingURL: String
)
