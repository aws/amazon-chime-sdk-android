/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.ClientMetricsCollector
import com.amazon.chime.sdk.media.clientcontroller.video.VideoClientController
import com.amazon.chime.sdk.media.clientcontroller.video.VideoClientObserver
import com.amazon.chime.sdk.session.MeetingSessionConfiguration

class DefaultAudioVideoController(
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientController: VideoClientController,
    private val videoClientObserver: VideoClientObserver
) : AudioVideoControllerFacade {

    override fun start() {
        audioClientController.start(
            configuration.urls.audioFallbackURL,
            configuration.urls.audioHostURL,
            configuration.meetingId,
            configuration.credentials.attendeeId,
            configuration.credentials.joinToken
        )
        videoClientController.start(
            configuration.meetingId,
            configuration.credentials.joinToken
        )
    }

    override fun stop() {
        audioClientController.stop()
        videoClientController.stopAndDestroy()
    }

    override fun startLocalVideo() {
        videoClientController.startLocalVideo()
    }

    override fun stopLocalVideo() {
        videoClientController.stopLocalVideo()
    }

    override fun startRemoteVideo() {
        videoClientController.startRemoteVideo()
    }

    override fun stopRemoteVideo() {
        videoClientController.stopRemoteVideo()
    }

    override fun addAudioVideoObserver(observer: AudioVideoObserver) {
        audioClientObserver.subscribeToAudioClientStateChange(observer)
        videoClientObserver.subscribeToVideoClientStateChange(observer)
    }

    override fun removeAudioVideoObserver(observer: AudioVideoObserver) {
        audioClientObserver.unsubscribeFromAudioClientStateChange(observer)
        videoClientObserver.unsubscribeFromVideoClientStateChange(observer)
    }

    override fun addMetricsObserver(observer: MetricsObserver) {
        clientMetricsCollector.subscribeToMetrics(observer)
    }

    override fun removeMetricsObserver(observer: MetricsObserver) {
        clientMetricsCollector.unsubscribeFromMetrics(observer)
    }
}
