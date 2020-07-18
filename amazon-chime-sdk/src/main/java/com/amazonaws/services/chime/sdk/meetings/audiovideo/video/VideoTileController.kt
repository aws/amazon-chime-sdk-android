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
     * To initialize anything related to VideoTileController
     */
    fun initialize()

    /**
     * To destroy anything related to VideoTileController
     */
    fun destroy()

    /**
     * Called whenever there is a new Video frame received for any of the attendee in the meeting
     *
     * @param frame: [Any] - A frame of video
     * @param videoId: [Int] - Unique id that belongs to video being transmitted
     * @param attendeeId: [String] - An id of user who is transmitting current frame
     * @param videoStreamContentHeight - Height of the video stream being transmitted
     * @param videoStreamContentWidth - Width of the video stream being transmitted
     * @param pauseState: [VideoPauseState] - Current pause state of the video being received
     */
    fun onReceiveFrame(
        frame: Any?,
        videoId: Int,
        attendeeId: String?,
        pauseState: VideoPauseState
    )
}
