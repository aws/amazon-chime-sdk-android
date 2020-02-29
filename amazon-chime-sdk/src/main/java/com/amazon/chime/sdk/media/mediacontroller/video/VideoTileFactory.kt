/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

interface VideoTileFactory {
    /**
     * Make a [VideoTile]
     *
     * @param tileId: Int - Tile ID
     * @param attendeeId: String? - Attendee ID
     *
     * @return [VideoTile] to use with [VideoTileController]
    */
    fun makeTile(tileId: Int, attendeeId: String?): VideoTile
}
