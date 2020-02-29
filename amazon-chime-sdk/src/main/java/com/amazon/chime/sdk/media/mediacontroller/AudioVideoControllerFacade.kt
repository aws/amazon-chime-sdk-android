/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

interface AudioVideoControllerFacade {
    /**
     * Starts audio and video.
     */
    fun start()

    /**
     * Stops audio and video.
     */
    fun stop()

    /**
     * Subscribe to audio, video, and connection events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to subscribe to events with.
     */
    fun addAudioVideoObserver(observer: AudioVideoObserver)

    /**
     * Unsubscribes from audio, video, and connection events by removing specified [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to unsubscribe from events with.
     */
    fun removeAudioVideoObserver(observer: AudioVideoObserver)

    /**
     * Subscribe to metrics events with an [MetricsObserver].
     *
     * @param observer: [MetricsObserver] - The observer to subscribe to events with.
     */
    fun addMetricsObserver(observer: MetricsObserver)

    /**
     * Unsubscribes from metrics by removing specified [MetricsObserver].
     *
     * @param observer: [MetricsObserver] - The observer to unsubscribe from events with.
     */
    fun removeMetricsObserver(observer: MetricsObserver)

    /**
     * Start local video.
     */
    fun startLocalVideo()

    /**
     * Stop local video.
     */
    fun stopLocalVideo()
}
