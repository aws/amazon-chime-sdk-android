/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

interface VideoTileFactory {
    fun makeTile(tileId: Int, attendeeId: String?): VideoTile
}
