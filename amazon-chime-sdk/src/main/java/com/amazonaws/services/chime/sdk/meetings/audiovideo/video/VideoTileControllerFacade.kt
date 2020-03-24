/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoTileControllerFacade] manages video tile binding, pausing, and resuming as well as subscribing
 * to video tile events by adding a [VideoTileObserver].
 */
interface VideoTileControllerFacade {
    /**
     * Binds the video rendering view to Video Tile. The view will start displaying the video frame
     * after the completion of this API.
     *
     * @param videoView: [VideoRenderView] - View to render the video. Application needs to create it
     * and pass to SDK.
     * @param tileId: [Int] - id of the tile which was passed to the application in [VideoTileObserver.onVideoTileAdded] .
     */
    fun bindVideoView(videoView: VideoRenderView, tileId: Int)

    /**
     * Unbinds the video rendering view from Video Tile. The view will stop displaying the video frame
     * after the completion of this API.
     *
     * @param tileId: [Int] - id of the tile which was passed to the application in [VideoTileObserver.onVideoTileAdded] .
     */
    fun unbindVideoView(tileId: Int)

    /**
     * Subscribe to Video Tile events with an [VideoTileObserver].
     *
     * @param observer: [VideoTileObserver] - The observer to subscribe to events with.
     */
    fun addVideoTileObserver(observer: VideoTileObserver)

    /**
     * Unsubscribes from Video Tile events by removing specified [VideoTileObserver].
     *
     * @param observer: [VideoTileObserver] - The observer to unsubscribe from events with.
     */
    fun removeVideoTileObserver(observer: VideoTileObserver)

    /**
     * Pauses the specified remote video tile. Ignores the tileId if it belongs to the local video tile.
     *
     * @param tileId: Int - The ID of the remote video tile to pause.
     */
    fun pauseRemoteVideoTile(tileId: Int)

    /**
     * Resumes the specified remote video tile. Ignores the tileId if it belongs to the local video tile.
     *
     * @param tileId: Int - The ID of the remote video tile to resume.
     */
    fun resumeRemoteVideoTile(tileId: Int)
}
