package com.amazon.chime.sdk.media.mediacontroller

interface AudioVideoControllerFacade {

    /**
     * Subscribe to audio, video, and connection events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to subscribe to events with.
     */
    fun addObserver(observer: AudioVideoObserver)

    /**
     * Unsubscribes from audio, video, and connection events by removing specified [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to unsubscribe from events with.
     */
    fun removeObserver(observer: AudioVideoObserver)

    /**
     * Starts audio and video.
     */
    fun start()

    /**
     * Stops audio and video.
     */
    fun stop()

    /**
     * Start local video.
     */
    fun startLocalVideo()

    /**
     * Stop local video.
     */
    fun stopLocalVideo()
}
