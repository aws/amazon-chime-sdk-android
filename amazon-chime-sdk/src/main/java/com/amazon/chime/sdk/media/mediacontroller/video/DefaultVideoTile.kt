/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.utils.logger.Logger
import com.amazon.chime.webrtc.EglBase
import com.amazon.chime.webrtc.VideoRenderer

class DefaultVideoTile(
    private val logger: Logger,
    tileId: Int,
    attendeeId: String?
) : VideoTile {
    private val TAG = "DefaultVideoTile"

    override var state: VideoTileState = VideoTileState(tileId, attendeeId, false)
    override var videoRenderView: DefaultVideoRenderView? = null

    override fun bind(rootEglBase: EglBase?, videoRenderView: DefaultVideoRenderView?) {
        logger.info(TAG, "Binding the View to Tile")
        videoRenderView?.init(rootEglBase?.eglBaseContext, null)
        this.videoRenderView = videoRenderView
    }

    override fun renderFrame(frame: Any?) {
        videoRenderView?.renderFrame(frame as VideoRenderer.I420Frame)
    }

    override fun unbind() {
        logger.info(TAG, "Unbinding the View from Tile")
        videoRenderView?.release()
        videoRenderView = null
    }

    override fun resume() {
        this.state.paused = false
    }

    override fun pause() {
        this.state.paused = true
    }
}
