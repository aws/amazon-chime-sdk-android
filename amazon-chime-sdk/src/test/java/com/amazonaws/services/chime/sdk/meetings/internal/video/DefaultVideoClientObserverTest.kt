/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
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

    private val turnRequestParams =
        TURNRequestParams(
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
        testVideoClientObserver =
            DefaultVideoClientObserver(
                mockLogger,
                turnRequestParams,
                mockMetricsCollector,
                mockVideoClientStateController
            )
        testVideoClientObserver.subscribeToVideoClientStateChange(mockAudioVideoObserver)
        testVideoClientObserver.subscribeToVideoTileChange(mockVideoTileController)
    }

    @Test
    fun `isConnecting should notify added observers about video session connecting event`() {
        testVideoClientObserver.isConnecting(mockVideoClient)

        verify { mockAudioVideoObserver.onVideoSessionStartedConnecting() }
    }

    @Test
    fun `didConnect should notify added observers about video session start when control status is OK`() {
        testVideoClientObserver.didConnect(mockVideoClient, videoClientSuccessCode)

        verify { mockAudioVideoObserver.onVideoSessionStarted(any()) }
    }

    @Test
    fun `didConnect should notify added observers about video session start with error status when control status is NOT OK`() {
        testVideoClientObserver.didConnect(
            mockVideoClient,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY_VIEW_ONLY
        )

        verify {
            mockAudioVideoObserver.onVideoSessionStarted(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoAtCapacityViewOnly
                )
            )
        }
    }

    @Test
    fun `didFail should notify added observers about video session failure event`() {
        testVideoClientObserver.didFail(
            mockVideoClient,
            videoClientSuccessCode,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY
        )

        verify {
            mockAudioVideoObserver.onVideoSessionStopped(
                MeetingSessionStatus(
                    MeetingSessionStatusCode.VideoServiceFailed
                )
            )
        }
    }

    @Test
    fun `didStop should notify added observers about video session stop event`() {
        testVideoClientObserver.didStop(mockVideoClient)

        verify {
            mockAudioVideoObserver.onVideoSessionStopped(
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

        verify(exactly = 0) { mockAudioVideoObserver.onVideoSessionStartedConnecting() }
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
