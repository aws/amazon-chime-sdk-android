/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.media.enums.VideoPauseState
import com.amazon.chime.sdk.utils.logger.Logger

class DefaultVideoTile(
    private val logger: Logger,
    tileId: Int,
    attendeeId: String?
) : VideoTile {
    private val TAG = "DefaultVideoTile"

    override var state: VideoTileState = VideoTileState(tileId, attendeeId, VideoPauseState.Unpaused)
    override var videoRenderView: VideoRenderView? = null

    override fun bind(bindParams: Any?, videoRenderView: VideoRenderView?) {
        logger.info(TAG, "Binding the View to Tile")
        videoRenderView?.initialize(bindParams)
        this.videoRenderView = videoRenderView
    }

    override fun renderFrame(frame: Any) {
        videoRenderView?.renderFrame(frame)
    }

    override fun unbind() {
        logger.info(TAG, "Unbinding the View from Tile")
        videoRenderView?.finalize()
        videoRenderView = null
    }

    override fun setPauseState(pauseState: VideoPauseState) {
        this.state.pauseState = pauseState
    }
}
