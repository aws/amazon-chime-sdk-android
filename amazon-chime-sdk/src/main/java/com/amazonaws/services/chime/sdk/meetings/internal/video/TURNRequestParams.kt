/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

/**
 * Data needed to make a request for [TURNCredentials].
 */
data class TURNRequestParams(
    val meetingId: String,
    val signalingUrl: String,
    val turnControlUrl: String,
    val joinToken: String
)
