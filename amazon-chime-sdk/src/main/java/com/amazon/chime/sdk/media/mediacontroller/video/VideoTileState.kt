/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.chime.sdk.media.mediacontroller.video

/**
 * @property titleId Unique Id associated with this tile
 * @property attendeeId Attendee Id of the user associated with this tile
 * @property paused Whether video tile has been paused
 */
data class VideoTileState(val tileId: Int, val attendeeId: String?, var paused: Boolean) {
    /**
     * Whether tile is local or remote tile
     * Local attendeeId is null because VideoClient doesn't know its attendeeId
     */
    val isLocalTile: Boolean = attendeeId == null
}
