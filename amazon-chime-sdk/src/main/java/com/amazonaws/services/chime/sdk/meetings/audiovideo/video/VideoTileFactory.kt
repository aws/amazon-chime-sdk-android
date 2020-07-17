/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

interface VideoTileFactory {
    /**
     * Make a [VideoTile]
     *
     * @param tileId: Int - Tile ID
     * @param attendeeId: String? - Attendee ID
     * @param videoStreamContentWidth: Int - Width of video stream content
     * @param videoStreamContentHeight: Int - Height of video stream content
     * @param isLocalTile: Boolean: Whether the video tile is for the local attendee
     *
     * @return [VideoTile] to use with [VideoTileController]
    */
    fun makeTile(
        tileId: Int,
        attendeeId: String,
        videoStreamContentWidth: Int,
        videoStreamContentHeight: Int,
        isLocalTile: Boolean
    ): VideoTile
}
