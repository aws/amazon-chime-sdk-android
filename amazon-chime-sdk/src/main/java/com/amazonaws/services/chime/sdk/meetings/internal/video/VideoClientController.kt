/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.xodee.client.video.VideoDevice

/**
 * [VideoClientController] uses the Video Client for video related functionality such as starting
 * local or remote video, switching camera, or pausing a remote video.
 */
interface VideoClientController {
    /**
     * Start the video client.
     *
     * @param meetingId: String - Meeting Id for the meeting session.
     * @param joinToken: String - Join token for the meeting session.
     */
    fun start(meetingId: String, joinToken: String)

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
}
