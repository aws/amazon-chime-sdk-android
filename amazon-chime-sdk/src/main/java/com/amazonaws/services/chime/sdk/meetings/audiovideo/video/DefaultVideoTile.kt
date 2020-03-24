/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

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
