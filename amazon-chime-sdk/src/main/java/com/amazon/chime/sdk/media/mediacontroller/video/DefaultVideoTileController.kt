/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.utils.logger.Logger
import com.amazon.chime.webrtc.EglBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultVideoTileController(
    private val logger: Logger,
    private val videoClientController: VideoClientController,
    private val videoTileFactory: VideoTileFactory
) : VideoTileController {
    private val NO_PAUSE = 0
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
        displayId: Int,
        pauseType: Int,
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
            if (frame != null) {
                tile.renderFrame(frame)
            } else if (pauseType == NO_PAUSE) {
                uiScope.launch {
                    logger.info(
                        TAG,
                        "Removing video tile with videoId = $videoId & attendeeId = $attendeeId"
                    )
                    onRemoveVideoTile(videoId)
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
            it.pause()
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
            it.resume()
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
            forEachObserver { observer -> observer.onRemoveVideoTile(it.state) }
        }
    }

    private fun onAddVideoTile(tileId: Int, attendeeId: String?) {
        val tile = videoTileFactory.makeTile(tileId, attendeeId)
        videoTileMap[tileId] = tile
        forEachObserver { observer -> observer.onAddVideoTile(tile.state) }
    }

    private fun forEachObserver(observerFunction: (observer: VideoTileObserver) -> Unit) {
        for (observer in videoTileObservers) {
            observerFunction(observer)
        }
    }
}
