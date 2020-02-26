/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

interface VideoTileObserver {

    /**
     * Called whenever an attendee starts sharing the video
     *
     * @param tile: [VideoTile] - Video tile associated with new attendee.
     */
    fun onAddVideoTrack(tile: VideoTile)

    /**
     * Called whenever any attendee stops sharing the video
     * @param tile: [VideoTile] - Video tile associated with attendee who is removed
     */
    fun onRemoveVideoTrack(tile: VideoTile)
}
