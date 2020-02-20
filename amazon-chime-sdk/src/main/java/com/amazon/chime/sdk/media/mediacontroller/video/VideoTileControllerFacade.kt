package com.amazon.chime.sdk.media.mediacontroller.video

interface VideoTileControllerFacade {

    /**
     * Binds the video rendering view to Video Tile. The view will start displaying the video frame
     * after the completion of this API
     *
     * @param videoView: [DefaultVideoRenderView] - View to render the video. Application needs to create it
     * and pass to SDK.
     * @param tileId: [Int] - id of the tile which was passed to the application in [VideoTileObserver.onAddVideoTrack] .
     */
    fun bindVideoView(videoView: DefaultVideoRenderView, tileId: Int)

    /**
     * Unbinds the video rendering view from Video Tile. The view will stop displaying the video frame
     * after the completion of this API
     *
     * @param tileId: [Int] - id of the tile which was passed to the application in [VideoTileObserver.onAddVideoTrack] .
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
}
