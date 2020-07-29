/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazon.chime.webrtc.EglBase
import com.amazon.chime.webrtc.VideoRenderer
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

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
        videoId: Int,
        attendeeId: String?,
        pauseState: VideoPauseState
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
        var videoStreamContentWidth = 0
        var videoStreamContentHeight = 0

        if (frame is VideoRenderer.I420Frame) {
            videoStreamContentWidth = frame.width
            videoStreamContentHeight = frame.height
        }

        if (tile != null) {
            if (frame == null && pauseState == VideoPauseState.Unpaused) {
                logger.info(
                    TAG,
                    "Removing video tile with videoId = $videoId & attendeeId = $attendeeId"
                )
                onRemoveVideoTile(videoId)
                return
            }

            if (videoStreamContentWidth != tile.state.videoStreamContentWidth ||
                videoStreamContentHeight != tile.state.videoStreamContentHeight) {
                tile.state.videoStreamContentWidth = videoStreamContentWidth
                tile.state.videoStreamContentHeight = videoStreamContentHeight
                forEachObserver { observer -> observer.onVideoTileSizeChanged(tile.state) }
            }

            // Account for any internally changed pause states, but ignore if the tile is paused by
            // user since the pause might not have propagated yet
            if (pauseState != tile.state.pauseState && tile.state.pauseState != VideoPauseState.PausedByUserRequest) {
                // Note that currently, since we preemptively mark tiles as PausedByUserRequest when requested by user
                // this path will only be hit when we are either transitioning from .unpaused to PausedForPoorConnection
                // or PausedForPoorConnection to Unpaused
                tile.setPauseState(pauseState)
                if (pauseState == VideoPauseState.Unpaused) {
                    forEachObserver { observer -> observer.onVideoTileResumed(tile.state) }
                } else {
                    forEachObserver { observer -> observer.onVideoTilePaused(tile.state) }
                }
            }

            // Ignore any frames which come to an already paused tile
            if (tile.state.pauseState == VideoPauseState.Unpaused) {
                frame?.run {
                    tile.renderFrame(frame)
                }
            }
        } else {
            frame?.run {
                logger.info(
                    TAG,
                    "Adding video tile with videoId = $videoId & attendeeId = $attendeeId"
                )
                onAddVideoTile(videoId, attendeeId, videoStreamContentWidth, videoStreamContentHeight)
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
                forEachObserver { observer -> observer.onVideoTilePaused(it.state) }
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
                forEachObserver { observer -> observer.onVideoTileResumed(it.state) }
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
            it.videoRenderView?.let {
                logger.info(TAG, "tileId = $tileId already had a different video view. Unbinding the old one and associating the new one")
                removeVideoViewFromMap(tileId)
            }
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

    private fun onAddVideoTile(tileId: Int, attendeeId: String?, videoStreamContentWidth: Int, videoStreamContentHeight: Int) {
        var isLocalTile: Boolean
        var thisAttendeeId: String

        if (attendeeId != null) {
            thisAttendeeId = attendeeId
            isLocalTile = false
        } else {
            thisAttendeeId = videoClientController.getConfiguration().credentials.attendeeId
            isLocalTile = true
        }
        val tile = videoTileFactory.makeTile(tileId, thisAttendeeId, videoStreamContentWidth, videoStreamContentHeight, isLocalTile)
        videoTileMap[tileId] = tile
        forEachObserver { observer -> observer.onVideoTileAdded(tile.state) }
    }

    private fun forEachObserver(observerFunction: (observer: VideoTileObserver) -> Unit) {
        ObserverUtils.notifyObserverOnMainThread(videoTileObservers, observerFunction)
    }
}
