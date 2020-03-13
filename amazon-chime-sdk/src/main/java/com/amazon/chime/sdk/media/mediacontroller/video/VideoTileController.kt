/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.media.enums.VideoPauseState

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
     * @param attendeeId: [String] - An id of user who is transmitting current frame
     * @param pauseState: [VideoPauseState] - Current pause state of the video being received
     * @param videoId: [Int] - Unique id that belongs to video being transmitted
     */
    fun onReceiveFrame(
        frame: Any?,
        attendeeId: String?,
        pauseState: VideoPauseState,
        videoId: Int
    )
}
