/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoTile] is a tile that binds video render view to display the frame into the view.
 */
interface VideoTile {
    /**
     * State of video tile
     */
    var state: VideoTileState

    /**
     * View which will be used to render the Video Frame
     */
    var videoRenderView: VideoRenderView?

    /**
     * Binds the view to the tile. The view needs to be create by the application.
     * Once the binding is done, the view will start displaying the video frame automatically
     *
     * @param bindParams: [Any] - Any parameter required to initialize render view
     * @param videoRenderView: [VideoRenderView] - The view created by application to render the video frame
     */
    fun bind(bindParams: Any?, videoRenderView: VideoRenderView?)

    /**
     * Renders the frame on [videoRenderView]. The call will be silently ignored if the view has not been bind
     * to the tile using [bind]
     *
     * @param frame: [Any] - Video Frame
     */
    fun renderFrame(frame: Any)

    /**
     * Unbinds the [videoRenderView] from tile.
     */
    fun unbind()

    /**
     * Update the pause state of the tile.
     */
    fun setPauseState(pauseState: VideoPauseState)
}
