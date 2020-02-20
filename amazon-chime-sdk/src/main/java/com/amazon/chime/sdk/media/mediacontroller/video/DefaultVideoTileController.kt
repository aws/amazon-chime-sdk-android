package com.amazon.chime.sdk.media.mediacontroller.video

import com.amazon.chime.sdk.utils.logger.Logger
import com.amazon.chime.webrtc.EglBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultVideoTileController(
    private val logger: Logger
) : VideoTileController {

    private val videoTileMap = mutableMapOf<Int, VideoTile>()
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
        val tile: VideoTile? = videoTileMap.get(videoId)
        if (tile != null) {
            if (frame != null) {
                tile.renderFrame(frame)
            } else {
                uiScope.launch {
                    logger.info(
                        TAG,
                        "Removing Video track with videoId = $videoId & attendeeId = $attendeeId"
                    )
                    onRemoveTrack(videoId)
                }
            }
        } else {
            if (frame != null) {
                uiScope.launch {
                    logger.info(
                        TAG,
                        "Adding Video track with videoId = $videoId & attendeeId = $attendeeId"
                    )
                    onAddTrack(videoId, attendeeId)
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

    override fun bindVideoView(videoView: DefaultVideoRenderView, tileId: Int) {
        logger.info(TAG, "Binding VideoView to Tile with tileId = $tileId")
        val tile: VideoTile? = videoTileMap.get(tileId)
        tile?.bind(rootEglBase, videoView)
    }

    override fun unbindVideoView(tileId: Int) {
        logger.info(TAG, "Unbinding Tile with tileId = $tileId")
        val tile: VideoTile? = videoTileMap.get(tileId)
        tile?.unbind()
        videoTileMap.remove(tileId)
    }

    private fun onRemoveTrack(tileId: Int) {
        val tile = videoTileMap.get(tileId)
        if (tile != null) {
            forEachObserver { observer -> observer.onRemoveVideoTrack(tile) }
        }
    }

    private fun onAddTrack(tileId: Int, profileId: String?) {
        val tile = DefaultVideoTile(logger, tileId, profileId)
        videoTileMap.set(tileId, tile)
        forEachObserver { observer -> observer.onAddVideoTrack(tile) }
    }

    private fun forEachObserver(observerFunction: (observer: VideoTileObserver) -> Unit) {
        for (observer in videoTileObservers) {
            observerFunction(observer)
        }
    }
}
