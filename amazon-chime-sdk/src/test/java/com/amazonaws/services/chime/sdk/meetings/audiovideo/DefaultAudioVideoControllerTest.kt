/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionURLs
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultAudioVideoControllerTest {
    @MockK
    private lateinit var audioVideo: AudioVideoObserver
    @MockK
    private lateinit var metricsObserver: MetricsObserver

    private val meetingId = "meetingId"
    private val attendeeId = "attendeeId"
    private val joinToken = "joinToken"
    private val audioFallbackURL = "audioFallbackURL"
    private val audioHostURL = "audioHostURL"
    private val turnControlURL = "turnControlURL"
    private val signalingURL = "signalingURL"

    private val meetingSessionConfiguration = MeetingSessionConfiguration(
        meetingId,
        MeetingSessionCredentials(attendeeId, joinToken),
        MeetingSessionURLs(audioFallbackURL, audioHostURL, turnControlURL, signalingURL)
    )

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var clientMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var videoClientObserver: VideoClientObserver

    private lateinit var audioVideoController: DefaultAudioVideoController

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                clientMetricsCollector,
                meetingSessionConfiguration,
                videoClientController,
                videoClientObserver
            )
    }

    @Test
    fun `start should call audioClientController start with the parameters in configuration`() {
        audioVideoController.start()
        verify {
            audioClientController.start(
                audioFallbackURL,
                audioHostURL,
                meetingId,
                attendeeId,
                joinToken
            )
        }
    }

    @Test
    fun `start should call videoClientController start with the parameters in configuration`() {
        audioVideoController.start()
        verify {
            videoClientController.start(
                meetingId,
                joinToken
            )
        }
    }

    @Test
    fun `stop should call audioClientController stop`() {
        audioVideoController.stop()

        verify { audioClientController.stop() }
    }

    @Test
    fun `stop should call videoClientController stopAndDestroy`() {
        audioVideoController.stop()

        verify { videoClientController.stopAndDestroy() }
    }

    @Test
    fun `addAudioVideoObserver should call audioClientObserver subscribeToAudioClientStateChange with given observer`() {
        audioVideoController.addAudioVideoObserver(audioVideo)

        verify { audioClientObserver.subscribeToAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `removeAudioVideoObserver should call audioClientObserver unsubscribeFromAudioClientStateChange with given observer`() {
        audioVideoController.removeAudioVideoObserver(audioVideo)

        verify { audioClientObserver.unsubscribeFromAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `addAudioVideoObserver should call videoClientObserver subscribeToVideoClientStateChange with given observer`() {
        audioVideoController.addAudioVideoObserver(audioVideo)

        verify { videoClientObserver.subscribeToVideoClientStateChange(audioVideo) }
    }

    @Test
    fun `removeAudioVideoObserver should call videoClientObserver unsubscribeFromAVideoClientStateChange with given observer`() {
        audioVideoController.removeAudioVideoObserver(audioVideo)

        verify { videoClientObserver.unsubscribeFromVideoClientStateChange(audioVideo) }
    }

    @Test
    fun `addMetricsObserver should call clientMetricsCollector addObserver with given observer`() {
        audioVideoController.addMetricsObserver(metricsObserver)

        verify { clientMetricsCollector.subscribeToMetrics(metricsObserver) }
    }

    @Test
    fun `removeMetricsObserver should call clientMetricsCollector removeObserver with given observer`() {
        audioVideoController.removeMetricsObserver(metricsObserver)

        verify { clientMetricsCollector.unsubscribeFromMetrics(metricsObserver) }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo`() {
        audioVideoController.startLocalVideo()

        verify { videoClientController.startLocalVideo() }
    }

    @Test
    fun `stopLocalVideo should call videoClientController stopLocalVideo`() {
        audioVideoController.stopLocalVideo()

        verify { videoClientController.stopLocalVideo() }
    }

    @Test
    fun `startRemoteVideo should call videoClientController startRemoteVideo`() {
        audioVideoController.startRemoteVideo()

        verify { videoClientController.startRemoteVideo() }
    }

    @Test
    fun `stopRemoteVideo should call videoClientController stopRemoteVideo`() {
        audioVideoController.stopRemoteVideo()

        verify { videoClientController.stopRemoteVideo() }
    }
}
