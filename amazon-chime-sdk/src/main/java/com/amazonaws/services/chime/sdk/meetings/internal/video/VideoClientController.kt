/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.xodee.client.video.VideoDevice

/**
 * [VideoClientController] uses the Video Client for video related functionality such as starting
 * local or remote video, switching camera, or pausing a remote video.
 */
interface VideoClientController {
    /**
     * Start the video client.
     */
    fun start()

    /**
     * Stop and destroy the video client.
     */
    fun stopAndDestroy()

    /**
     * Starts sending video for local attendee.
     */
    fun startLocalVideo()

    /**
     * Stops sending video for local attendee.
     */
    fun stopLocalVideo()

    /**
     * Starts receiving video from remote attendee(s).
     */
    fun startRemoteVideo()

    /**
     * Stops receiving video from remote attendee(s).
     */
    fun stopRemoteVideo()

    /**
     * Get the currently active camera, if any.
     *
     * @return [VideoDevice] - Information about the current active device used for video.
     */
    fun getActiveCamera(): VideoDevice?

    /**
     * Switches the currently active camera.
     */
    fun switchCamera()

    /**
     * Pause or resume a remote video tile.
     *
     * @param isPaused: Boolean - Whether or not the tile should be paused.
     * @param videoId: Int - Id of the remote video tile to pause or resume.
     */
    fun setRemotePaused(isPaused: Boolean, videoId: Int)

    /**
     * Get the meeting session configuration
     *
     * @return [MeetingSessionConfiguration] - Configuration of current meeting session
     */
    fun getConfiguration(): MeetingSessionConfiguration

    /**
     * Send message via data channel
     *
     * @param topic: String - topic the message is sent to
     * @param data: Any - data payload, it can be ByteArray, String or other serializable object,
     * which will be convert to ByteArray
     * @param lifetimeMs: Int - the milliseconds of lifetime that is available to late subscribers
     * @throws [InvalidParameterException] when topic is not match regex "^[a-zA-Z0-9_-]{1,36}$",
     * or data size is over 2kb, or lifetime ms is negative
     */
    fun sendDataMessage(topic: String, data: Any, lifetimeMs: Int)
}
