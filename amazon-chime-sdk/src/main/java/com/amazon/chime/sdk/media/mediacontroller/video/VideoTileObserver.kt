/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

/**
 * [VideoTileObserver] handles events related to [VideoTile].
 */
interface VideoTileObserver {

    /**
     * Called whenever an attendee starts sharing the video
     *
     * @param tileState: [VideoTileState] - Video tile state associated with new attendee.
     */
    fun onAddVideoTile(tileState: VideoTileState)

    /**
     * Called whenever any attendee stops sharing the video
     * @param tileState: [VideoTileState] - Video tile state associated with attendee who is removed
     */
    fun onRemoveVideoTile(tileState: VideoTileState)
}
