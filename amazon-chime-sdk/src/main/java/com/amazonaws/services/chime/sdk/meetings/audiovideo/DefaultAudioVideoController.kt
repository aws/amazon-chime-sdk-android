/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration

class DefaultAudioVideoController(
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val configuration: MeetingSessionConfiguration,
    private val videoClientController: VideoClientController,
    private val videoClientObserver: VideoClientObserver
) : AudioVideoControllerFacade {
    override fun start() {
        start(AudioVideoConfiguration())
    }

    override fun start(audioVideoConfiguration: AudioVideoConfiguration) {
        audioClientController.start(
            audioFallbackUrl = configuration.urls.audioFallbackURL,
            audioHostUrl = configuration.urls.audioHostURL,
            meetingId = configuration.meetingId,
            attendeeId = configuration.credentials.attendeeId,
            joinToken = configuration.credentials.joinToken,
            audioMode = audioVideoConfiguration.audioMode
        )
        videoClientController.start()
    }

    override fun stop() {
        audioClientController.stop()
        videoClientController.stopAndDestroy()
    }

    override fun startLocalVideo() {
        videoClientController.startLocalVideo()
    }

    override fun startLocalVideo(source: VideoSource) {
        videoClientController.startLocalVideo(source)
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

    override fun updateVideoSourceSubscriptions(
        addedOrUpdated: Map<RemoteVideoSource, VideoSubscriptionConfiguration>,
        removed: Array<RemoteVideoSource>
    ) {
        videoClientController.updateVideoSourceSubscriptions(addedOrUpdated, removed)
    }
}
