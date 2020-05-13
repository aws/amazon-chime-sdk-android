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
    private val _audioFallbackURL: String,
    private val _audioHostURL: String,
    private val _turnControlURL: String,
    private val _signalingURL: String,
    val urlRewriter: URLRewriter
) {
    val audioHostURL = urlRewriter(_audioHostURL)
    val audioFallbackURL = urlRewriter(_audioFallbackURL)
    val turnControlURL = urlRewriter(_turnControlURL)
    val signalingURL = urlRewriter(_signalingURL)
}
