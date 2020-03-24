/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * Contains properties related to the current state of the [VideoTile]
 *
 * @property tileId: Int - Unique Id associated with this tile
 * @property attendeeId: String? - Attendee Id of the user associated with this tile
 * @property pauseState: VideoPauseState - The current pause state of the tile
 * @property isLocalTile: Boolean - Whether the video tile is for the local attendee
 */
data class VideoTileState(val tileId: Int, val attendeeId: String?, var pauseState: VideoPauseState) {
    /**
     * Whether the video tile is for the local attendee
     */
    val isLocalTile: Boolean = attendeeId == null

    /**
     * Whether the video tile is from screen share
     */
    val isContent: Boolean = attendeeId?.endsWith("#content") ?: false
}
