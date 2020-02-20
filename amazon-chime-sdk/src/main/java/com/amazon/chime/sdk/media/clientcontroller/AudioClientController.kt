/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

/**
 * [AudioClientController]'s responsibility is to handle AudioClient API calls such as starting
 * and stopping audio session
 *
 * Interface was created in response to difficulty in refactoring [AudioClientController] into
 * [DefaultAudioClientController] and [DefaultAudioClientObserver] without breaking the rest of code
 */
interface AudioClientController {
    fun getRoute(): Int
    fun setRoute(route: Int): Boolean
    fun start(audioFallbackUrl: String, audioHostUrl: String, meetingId: String, attendeeId: String, joinToken: String)
    fun stop()
    fun setMute(isMuted: Boolean): Boolean
}
