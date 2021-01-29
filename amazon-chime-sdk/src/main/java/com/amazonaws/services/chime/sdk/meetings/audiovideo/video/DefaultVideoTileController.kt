/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultVideoTileController(
    private val logger: Logger,
    private val videoClientController: VideoClientController,
    private val videoTileFactory: VideoTileFactory,
    private val eglCoreFactory: EglCoreFactory,
    private val meetingStatsCollector: MeetingStatsCollector
) : VideoTileController {
    // A map of tile id to VideoTile to determine if VideoTileController is adding, removing, pausing, or rendering
    private val videoTileMap = mutableMapOf<Int, VideoTile>()
    // A map of VideoRenderView to VideoTile to determine if users are adding same video render view
    private val renderViewToBoundVideoTileMap = mutableMapOf<VideoRenderView, VideoTile>()
    private val TAG = "DefaultVideoTileController"

    private var videoTileObservers = mutableSetOf<VideoTileObserver>()

    override fun onReceiveFrame(
        frame: VideoFrame?,
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

        val videoStreamContentWidth = frame?.width ?: 0
        val videoStreamContentHeight = frame?.height ?: 0

        if (tile != null) {
            if (frame == null && pauseState == VideoPauseState.Unpaused) {
                logger.info(
                    TAG,
                    "Removing video tile with videoId = $videoId & attendeeId = $attendeeId"
                )
                onRemoveVideoTile(videoId)
                return
            }

            if (frame != null && (videoStreamContentWidth != tile.state.videoStreamContentWidth ||
                videoStreamContentHeight != tile.state.videoStreamContentHeight)) {
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
                frame?.run { tile.onVideoFrameReceived(frame) }
            } else {
                logger.verbose(TAG, "Ignoring video frame received on paused tile")
            }
        } else {
            if (frame != null || pauseState != VideoPauseState.Unpaused) {
                run {
                    logger.info(
                        TAG,
                        "Adding video tile with videoId = $videoId, attendeeId = $attendeeId, pauseState = $pauseState"
                    )
                    onAddVideoTile(videoId, attendeeId, pauseState, videoStreamContentWidth, videoStreamContentHeight)
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

        renderViewToBoundVideoTileMap[videoView]?.let {
            logger.warn(TAG, "Override the binding from ${it.state.tileId} to $tileId")
            removeRenderViewFromBoundVideoTileMap(it.state.tileId)
        }

        videoTileMap[tileId]?.let {
            it.videoRenderView?.let {
                logger.info(TAG, "tileId = $tileId already had a different video view. Unbinding the old one and associating the new one")
                removeRenderViewFromBoundVideoTileMap(tileId)
            }
            if (videoView is EglVideoRenderView) {
                logger.info(TAG, "Initializing EGL state on EGL render view")
                videoView.init(eglCoreFactory)
            }
            it.bind(videoView)
            renderViewToBoundVideoTileMap[videoView] = it
        }
    }

    private fun removeRenderViewFromBoundVideoTileMap(tileId: Int) {
        renderViewToBoundVideoTileMap.entries.firstOrNull { it.value.state.tileId == tileId }?.let {
            val renderView = it.key
            val videoTile = it.value
            videoTile.unbind()
            if (renderView is EglVideoRenderView) {
                logger.info(TAG, "Releasing EGL state on EGL render view")
                renderView.release()
            }
            renderViewToBoundVideoTileMap.remove(renderView)
        }
    }

    override fun unbindVideoView(tileId: Int) {
        logger.info(TAG, "Unbinding Tile with tileId = $tileId")
        // Remove the video from both mappings when unbind, in order to keep the old SDK behavior
        videoTileMap.remove(tileId)
        removeRenderViewFromBoundVideoTileMap(tileId)
    }

    private fun onRemoveVideoTile(tileId: Int) {
        videoTileMap[tileId]?.let {
            forEachObserver { observer -> observer.onVideoTileRemoved(it.state) }
            videoTileMap.remove(tileId)
        }
    }

    private fun onAddVideoTile(tileId: Int, attendeeId: String?, pauseState: VideoPauseState, videoStreamContentWidth: Int, videoStreamContentHeight: Int) {
        val isLocalTile: Boolean
        val thisAttendeeId: String

        if (attendeeId != null) {
            thisAttendeeId = attendeeId
            isLocalTile = false
        } else {
            thisAttendeeId = videoClientController.getConfiguration().credentials.attendeeId
            isLocalTile = true
        }
        val tile = videoTileFactory.makeTile(tileId, thisAttendeeId, videoStreamContentWidth, videoStreamContentHeight, isLocalTile)
        videoTileMap[tileId] = tile
        meetingStatsCollector.updateMaxVideoTile(videoTileMap.size)
        tile.setPauseState(pauseState)
        forEachObserver { observer -> observer.onVideoTileAdded(tile.state) }
    }

    private fun forEachObserver(observerFunction: (observer: VideoTileObserver) -> Unit) {
        ObserverUtils.notifyObserverOnMainThread(videoTileObservers, observerFunction)
    }
}
