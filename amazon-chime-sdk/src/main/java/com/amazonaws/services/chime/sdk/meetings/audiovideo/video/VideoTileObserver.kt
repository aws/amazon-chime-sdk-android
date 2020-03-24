/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

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

    /**
     * Called whenever an attendee tile pauseState changes from VideoPauseState.Unpaused
     * @param tileState: [VideoTileState] - Video tile state associated with attendee who is paused
     */
    fun onPauseVideoTile(tileState: VideoTileState)

    /**
     * Called whenever an attendee tile pauseState changes to VideoPauseState.Unpaused
     * @param tileState: [VideoTileState] - Video tile state associated with attendee who is resumed
     */
    fun onResumeVideoTile(tileState: VideoTileState)
}
