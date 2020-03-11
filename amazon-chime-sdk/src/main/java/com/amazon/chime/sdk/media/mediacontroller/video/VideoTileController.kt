/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

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
     * @param frame: [Any] - Video frame
     * @param attendeeId: [String] - Attendee Id
     * @param displayId: [Int] - display Id
     * @param pauseType: [Int] - pauseType
     * @param videoId: [Int] - Video Id
     */
    fun onReceiveFrame(
        frame: Any?,
        attendeeId: String?,
        displayId: Int,
        pauseType: Int,
        videoId: Int
    )
}
