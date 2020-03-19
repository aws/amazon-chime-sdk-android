/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller.video

/**
 * Data needed to make a request for [TURNCredentials].
 */
data class TURNRequestParams(
    val meetingId: String,
    val signalingUrl: String,
    val turnControlUrl: String,
    val joinToken: String
)
