/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.utils.logger.Logger

class DefaultVideoTileFactory(private val logger: Logger) : VideoTileFactory {
    override fun makeTile(tileId: Int, attendeeId: String?): VideoTile {
        return DefaultVideoTile(logger, tileId, attendeeId)
    }
}
