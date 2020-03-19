/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller.video

import com.amazon.chime.sdk.media.clientcontroller.ClientMetricsCollector
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileController
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultVideoClientObserverTest {
    @MockK
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var mockVideoTileController: VideoTileController

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var mockVideoClientStateController: VideoClientStateController

    @MockK
    private lateinit var mockVideoClient: VideoClient

    private lateinit var testVideoClientObserver: DefaultVideoClientObserver

    private val turnRequestParams = TURNRequestParams(
        "meetingId",
        "signalingUrl",
        "turnControlUrl",
        "joinToken"
    )

    private val videoClientSuccessCode = 0
    private val testMessage = "Hello world"
    private val testFrame = "I am the frame"
    private val testProfileId = "aliceId"
    private val testVideoId = 1

    @Before
    fun setUp() {
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } just runs
        MockKAnnotations.init(this, relaxUnitFun = true)
        testVideoClientObserver = DefaultVideoClientObserver(
            mockLogger,
            turnRequestParams,
            mockMetricsCollector,
            mockVideoClientStateController
        )
        testVideoClientObserver.subscribeToVideoClientStateChange(mockAudioVideoObserver)
        testVideoClientObserver.subscribeToVideoTileChange(mockVideoTileController)
    }

    @Test
    fun `isConnecting should notify added observers about video client connecting event`() {
        testVideoClientObserver.isConnecting(mockVideoClient)

        verify { mockAudioVideoObserver.onVideoClientConnecting() }
    }

    @Test
    fun `didConnect should notify added observers about video client start event when control status is OK`() {
        testVideoClientObserver.didConnect(mockVideoClient, videoClientSuccessCode)

        verify { mockAudioVideoObserver.onVideoClientStart() }
    }

    @Test
    fun `didConnect should notify added observers about video client error when control status is NOT OK`() {
        testVideoClientObserver.didConnect(
            mockVideoClient,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY_VIEW_ONLY
        )

        verify {
            mockAudioVideoObserver.onVideoClientError(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoAtCapacityViewOnly
                )
            )
        }
    }

    @Test
    fun `didFail should notify added observers about video client failure event`() {
        testVideoClientObserver.didFail(
            mockVideoClient,
            videoClientSuccessCode,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY
        )

        verify {
            mockAudioVideoObserver.onVideoClientStop(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoServiceFailed
                )
            )
        }
    }

    @Test
    fun `didStop should notify added observers about video client stop event`() {
        testVideoClientObserver.didStop(mockVideoClient)

        verify {
            mockAudioVideoObserver.onVideoClientStop(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.OK
                )
            )
        }
    }

    @Test
    fun `didReceiveFrame should notify added observers about frame receive event`() {
        testVideoClientObserver.didReceiveFrame(
            mockVideoClient,
            testFrame,
            testProfileId,
            testVideoId,
            VideoClient.VIDEO_CLIENT_NO_PAUSE,
            testVideoId
        )

        verify { mockVideoTileController.onReceiveFrame(any(), any(), any(), any()) }
    }

    @Test
    fun `onMetrics should call clientMetricsCollector to process video metrics`() {
        testVideoClientObserver.onMetrics(intArrayOf(1), doubleArrayOf(1.0))

        verify { mockMetricsCollector.processVideoClientMetrics(any()) }
    }

    @Test
    fun `onLogMessage should call logger`() {
        testVideoClientObserver.onLogMessage(AudioClient.L_ERROR, testMessage)

        verify { mockLogger.error(any(), testMessage) }
    }

    @Test
    fun `onLogMessage should NOT call logger when message is null`() {
        testVideoClientObserver.onLogMessage(AudioClient.L_FATAL, null)

        verify(exactly = 0) { mockLogger.error(any(), any()) }
    }

    @Test
    fun `onLogMessage should NOT call logger when log level is non error or non fatal`() {
        testVideoClientObserver.onLogMessage(AudioClient.L_INFO, testMessage)

        verify(exactly = 0) { mockLogger.error(any(), testMessage) }
    }

    @Test
    fun `unsubscribeFromVideoClientStateChange should result in no notification`() {
        testVideoClientObserver.unsubscribeFromVideoClientStateChange(mockAudioVideoObserver)

        testVideoClientObserver.isConnecting(mockVideoClient)

        verify(exactly = 0) { mockAudioVideoObserver.onVideoClientConnecting() }
    }

    @Test
    fun `unsubscribeFromVideoTile should result in no notification`() {
        testVideoClientObserver.unsubscribeFromVideoTileChange(mockVideoTileController)

        testVideoClientObserver.didReceiveFrame(
            mockVideoClient,
            testFrame,
            testProfileId,
            testVideoId,
            VideoClient.VIDEO_CLIENT_NO_PAUSE,
            testVideoId
        )

        verify(exactly = 0) { mockVideoTileController.onReceiveFrame(any(), any(), any(), any()) }
    }
}
