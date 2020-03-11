/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.webrtc.EglBase

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
     * @param rootEglBase: [EglBase] - Shared EGL context
     * @param videoRenderView: [DefaultVideoRenderView] - The view created by application to render the video frame
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
     * Unbinds the [videoRenderView] from tile. Any EGL context associated with the [videoRenderView] will be released
     */
    fun unbind()

    /**
     * Pauses the tile. When paused, the tile moves to an inactive state and will not receive
     * frame update callback
     */
    fun pause()

    /**
     * Resume the tile if it was paused. When resumed,
     * the tile moves to the active state.
     */
    fun resume()
}
