/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazon.chime.webrtc.EglBase
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultVideoTileController(
    private val logger: Logger,
    private val videoClientController: VideoClientController,
    private val videoTileFactory: VideoTileFactory
) : VideoTileController {
    // A map of tile id to VideoTile to determine if VideoTileController is adding, removing, pausing, or rendering
    private val videoTileMap = mutableMapOf<Int, VideoTile>()
    // A map of VideoRenderView to tile id to determine if users are adding same video render view
    private val boundVideoViewMap = mutableMapOf<VideoRenderView, Int>()
    private val TAG = "DefaultVideoTileController"
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var videoTileObservers = mutableSetOf<VideoTileObserver>()
    private var rootEglBase: EglBase? = null

    override fun initialize() {
        logger.info(TAG, "initializing VideoTileController")
        rootEglBase = EglBase.create()
    }

    override fun destroy() {
        logger.info(TAG, "destroying VideoTileController")
        rootEglBase?.release()
    }

    override fun onReceiveFrame(
        frame: Any?,
        attendeeId: String?,
        pauseState: VideoPauseState,
        videoId: Int
    ) {
        /**
         * There are FOUR possible outcomes:
         * 1) Create - Someone has started sharing video
         * 2) Render / Resume - Someone is sending new frames for their video
         * 3) Pause - Someone is sending a pause frame
         * 4) Stop - Someone has stopped sharing video
         *
         * In both pause and stop cases, the frame is null but the pauseType differs
         */
        val tile: VideoTile? = videoTileMap[videoId]
        if (tile != null) {
            // Account for any internally changed pause states, but ignore if the tile is paused by
            // user since the pause might not have propagated yet
            if (pauseState != tile.state.pauseState && tile.state.pauseState != VideoPauseState.PausedByUserRequest) {
                // Note that currently, since we preemptively mark tiles as PausedByUserRequest when requested by user
                // this path will only be hit when we are either transitioning from .unpaused to PausedForPoorConnection
                // or PausedForPoorConnection to Unpaused
                tile.setPauseState(pauseState)
                if (pauseState == VideoPauseState.Unpaused) {
                    uiScope.launch {
                        forEachObserver { observer -> observer.onVideoTileResumed(tile.state) }
                    }
                } else {
                    uiScope.launch {
                        forEachObserver { observer -> observer.onVideoTilePaused(tile.state) }
                    }
                }
            }

            // Ignore any frames which come to an already paused tile
            if (tile.state.pauseState == VideoPauseState.Unpaused) {
                if (frame != null) {
                    tile.renderFrame(frame)
                } else {
                    uiScope.launch {
                        logger.info(
                            TAG,
                            "Removing video tile with videoId = $videoId & attendeeId = $attendeeId"
                        )
                        onRemoveVideoTile(videoId)
                    }
                }
            }
        } else {
            frame?.run {
                uiScope.launch {
                    logger.info(
                        TAG,
                        "Adding video tile with videoId = $videoId & attendeeId = $attendeeId"
                    )
                    onAddVideoTile(videoId, attendeeId)
                }
            }
        }
    }

    override fun addVideoTileObserver(observer: VideoTileObserver) {
        videoTileObservers.add(observer)
    }

    override fun removeVideoTileObserver(observer: VideoTileObserver) {
        videoTileObservers.remove(observer)
    }

    override fun pauseRemoteVideoTile(tileId: Int) {
        videoTileMap[tileId]?.let {
            if (it.state.isLocalTile) {
                logger.warn(TAG, "Cannot pause local video tile $tileId!")
                return
            }

            logger.info(TAG, "Pausing tile $tileId")
            videoClientController.setRemotePaused(
                true,
                tileId
            )
            // Don't update state/observers if we haven't changed anything
            // Note that this will overwrite PausedForPoorConnection if that is the current state
            if (it.state.pauseState != VideoPauseState.PausedByUserRequest) {
                it.setPauseState(VideoPauseState.PausedByUserRequest)
                uiScope.launch {
                    forEachObserver { observer -> observer.onVideoTilePaused(it.state) }
                }
            }
        }
    }

    override fun resumeRemoteVideoTile(tileId: Int) {
        videoTileMap[tileId]?.let {
            if (it.state.isLocalTile) {
                logger.warn(TAG, "Cannot resume local video tile $tileId!")
                return
            }

            logger.info(TAG, "Resuming tile $tileId")
            videoClientController.setRemotePaused(
                false,
                tileId
            )
            // Only update state if we are unpausing a tile which was previously paused by the user
            // Note that this means resuming a tile with state PausedForPoorConnection will no-op
            if (it.state.pauseState == VideoPauseState.PausedByUserRequest) {
                it.setPauseState(VideoPauseState.Unpaused)
                uiScope.launch {
                    forEachObserver { observer -> observer.onVideoTileResumed(it.state) }
                }
            }
        }
    }

    override fun bindVideoView(videoView: VideoRenderView, tileId: Int) {
        logger.info(TAG, "Binding VideoView to Tile with tileId = $tileId")

        boundVideoViewMap[videoView]?.let {
            logger.warn(TAG, "Override the binding from $it to $tileId")
            removeVideoViewFromMap(it)
        }

        videoTileMap[tileId]?.let {
            it.bind(rootEglBase, videoView)
            boundVideoViewMap[videoView] = tileId
        }
    }

    private fun removeVideoViewFromMap(tileId: Int) {
        videoTileMap[tileId]?.let {
            val renderView = it.videoRenderView
            it.unbind()
            boundVideoViewMap.remove(renderView)
        }
    }

    override fun unbindVideoView(tileId: Int) {
        logger.info(TAG, "Unbinding Tile with tileId = $tileId")
        removeVideoViewFromMap(tileId)
        videoTileMap.remove(tileId)
    }

    private fun onRemoveVideoTile(tileId: Int) {
        videoTileMap[tileId]?.let {
            forEachObserver { observer -> observer.onVideoTileRemoved(it.state) }
        }
    }

    private fun onAddVideoTile(tileId: Int, attendeeId: String?) {
        val tile = videoTileFactory.makeTile(tileId, attendeeId)
        videoTileMap[tileId] = tile
        forEachObserver { observer -> observer.onVideoTileAdded(tile.state) }
    }

    private fun forEachObserver(observerFunction: (observer: VideoTileObserver) -> Unit) {
        for (observer in videoTileObservers) {
            observerFunction(observer)
        }
    }
}
