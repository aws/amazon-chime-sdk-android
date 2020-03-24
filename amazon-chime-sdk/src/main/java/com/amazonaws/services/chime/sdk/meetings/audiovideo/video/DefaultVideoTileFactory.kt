/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultVideoTileFactory(private val logger: Logger) : VideoTileFactory {
    override fun makeTile(tileId: Int, attendeeId: String?): VideoTile {
        return DefaultVideoTile(logger, tileId, attendeeId)
    }
}
