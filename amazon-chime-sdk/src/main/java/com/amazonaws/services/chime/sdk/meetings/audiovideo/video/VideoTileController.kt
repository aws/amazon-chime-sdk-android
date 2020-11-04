/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoTileController] handles rendering/creating of new [VideoTile].
 */
interface VideoTileController : VideoTileControllerFacade {
    /**
     * Called whenever there is a new Video frame received for any of the attendee in the meeting
     *
     * @param frame: [VideoFrame] - A frame of video
     * @param videoId: [Int] - Unique id that belongs to video being transmitted
     * @param attendeeId: [String] - An id of user who is transmitting current frame
     * @param pauseState: [VideoPauseState] - Current pause state of the video being received
     */
    fun onReceiveFrame(
        frame: VideoFrame?,
        videoId: Int,
        attendeeId: String?,
        pauseState: VideoPauseState
    )
}
