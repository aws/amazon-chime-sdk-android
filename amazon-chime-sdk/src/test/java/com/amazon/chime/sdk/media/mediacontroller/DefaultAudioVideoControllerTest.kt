/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
import com.amazon.chime.sdk.media.clientcontroller.ClientMetricsCollector
import com.amazon.chime.sdk.media.clientcontroller.VideoClientController
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.session.MeetingSessionCredentials
import com.amazon.chime.sdk.session.MeetingSessionURLs
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
                videoClientController
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
    fun `stop should call audioClientController stop`() {
        audioVideoController.stop()
        verify { audioClientController.stop() }
    }

    @Test
    fun `addAudioVideoObserver should call audioClientObserver subscribeToAudioClientStateChange with given observer`() {
        audioVideoController.addAudioVideoObserver(audioVideo)
        verify { audioClientObserver.subscribeToAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `removeObserver should call audioClientObserver unsubscribeFromAudioClientStateChange with given observer`() {
        audioVideoController.removeAudioVideoObserver(audioVideo)
        verify { audioClientObserver.unsubscribeFromAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `addMetricsObserver should call clientMetricsCollector addObserver with given observer`() {
        audioVideoController.addMetricsObserver(metricsObserver)
        verify { clientMetricsCollector.subscribeToMetrics(metricsObserver) }
    }

    @Test
    fun `addMetricsObserver should call clientMetricsCollector removeObserver with given observer`() {
        audioVideoController.removeMetricsObserver(metricsObserver)
        verify { clientMetricsCollector.unsubscribeFromMetrics(metricsObserver) }
    }

    @Test
    fun `startLocalVideo should call videoClientController enableSelfVideo`() {
        audioVideoController.startLocalVideo()

        verify { videoClientController.enableSelfVideo(true) }
    }

    @Test
    fun `stopLocalVideo should call videoClientController enableSelfVideo`() {
        audioVideoController.stopLocalVideo()

        verify { videoClientController.enableSelfVideo(false) }
    }
}
