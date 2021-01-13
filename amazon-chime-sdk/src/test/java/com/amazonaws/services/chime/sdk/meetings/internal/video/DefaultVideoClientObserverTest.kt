/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.DataMessage
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DefaultVideoClientObserverTest {
    @MockK
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var mockVideoTileController: VideoTileController

    @MockK
    private lateinit var mockDataMessageObserver: DataMessageObserver

    @MockK
    private lateinit var mockAnotherDataMessageObserver: DataMessageObserver

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var mockVideoClientStateController: VideoClientStateController

    @MockK
    private lateinit var mockDefaultUrlRewriter: URLRewriter

    @MockK
    private lateinit var mockVideoClient: VideoClient

    @MockK(relaxed = true)
    private lateinit var mockMediaVideoFrame: com.xodee.client.video.VideoFrame

    @MockK(relaxed = true)
    private lateinit var mockMediaVideoFrameI420Buffer: com.xodee.client.video.VideoFrameI420Buffer

    @MockK(relaxed = true)
    private lateinit var mockMediaVideoFrameTextureBuffer: com.xodee.client.video.VideoFrameTextureBuffer

    // Required even if not queried
    private val testDataY = ByteBuffer.allocateDirect(0)
    private val testDataU = ByteBuffer.allocateDirect(0)
    private val testDataV = ByteBuffer.allocateDirect(0)

    private val testTimestamp: Long = 1
    private val testRotation = VideoRotation.Rotation90

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
    private val testProfileId = "aliceId"
    private val testVideoId = 1
    private val testDispatcher = TestCoroutineDispatcher()

    private val topic = "topic1"
    private val anotherTopic = "topic2"
    private val dataMessageWithTopic = DataMessage(
        10000,
        topic,
        "hello".toByteArray(),
        "attendeeId",
        "externalId",
        false
    )
    private val dataMessageWithAnotherTopic = DataMessage(
        10000,
        anotherTopic,
        "hello".toByteArray(),
        "attendeeId",
        "externalId",
        false
    )
    private val uri1 = "one"
    private val uri2 = "two"
    private val uri3 = "three"
    private val turnUris = listOf<String>(uri1, uri2, uri3)

    @Before
    fun setUp() {
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } just runs
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        testVideoClientObserver =
            DefaultVideoClientObserver(
                mockContext,
                mockLogger,
                turnRequestParams,
                mockMetricsCollector,
                mockVideoClientStateController,
                mockDefaultUrlRewriter
            )
        testVideoClientObserver.subscribeToVideoClientStateChange(mockAudioVideoObserver)
        testVideoClientObserver.subscribeToVideoTileChange(mockVideoTileController)

        every { mockMediaVideoFrameI420Buffer.dataY } returns testDataY
        every { mockMediaVideoFrameI420Buffer.dataU } returns testDataU
        every { mockMediaVideoFrameI420Buffer.dataV } returns testDataV
        every { mockMediaVideoFrame.timestampNs } returns testTimestamp
        every { mockMediaVideoFrame.rotation } returns 90
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
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
    fun `didReceiveFrame should notify added observers about frame receive event and maintains timestamp and rotation`() {
        every { mockMediaVideoFrame.buffer } returns mockMediaVideoFrameI420Buffer
        testVideoClientObserver.didReceiveFrame(
            mockVideoClient,
            mockMediaVideoFrame,
            testProfileId,
            testVideoId,
            VideoClient.VIDEO_CLIENT_NO_PAUSE,
            testVideoId
        )

        val slot = slot<VideoFrame>()
        verify { mockVideoTileController.onReceiveFrame(capture(slot), any(), any(), any()) }
        Assert.assertEquals(slot.captured.rotation, testRotation)
        Assert.assertEquals(slot.captured.timestampNs, testTimestamp)
    }

    @Test
    fun `didReceiveFrame should convert Media I420 frame to SDK I420 frame`() {
        every { mockMediaVideoFrame.buffer } returns mockMediaVideoFrameI420Buffer
        testVideoClientObserver.didReceiveFrame(
                mockVideoClient,
                mockMediaVideoFrame,
                testProfileId,
                testVideoId,
                VideoClient.VIDEO_CLIENT_NO_PAUSE,
                testVideoId
        )

        val slot = slot<VideoFrame>()
        verify { mockVideoTileController.onReceiveFrame(capture(slot), any(), any(), any()) }
        assert(slot.captured.buffer is VideoFrameI420Buffer)
    }

    @Test
    fun `didReceiveFrame should convert Media texture frame buffer to SDK texture frame buffer`() {
        every { mockMediaVideoFrame.buffer } returns mockMediaVideoFrameTextureBuffer
        testVideoClientObserver.didReceiveFrame(
                mockVideoClient,
                mockMediaVideoFrame,
                testProfileId,
                testVideoId,
                VideoClient.VIDEO_CLIENT_NO_PAUSE,
                testVideoId
        )

        val slot = slot<VideoFrame>()
        verify { mockVideoTileController.onReceiveFrame(capture(slot), any(), any(), any()) }
        assert(slot.captured.buffer is VideoFrameTextureBuffer)
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
        every { mockMediaVideoFrame.buffer } returns mockMediaVideoFrameI420Buffer

        testVideoClientObserver.unsubscribeFromVideoTileChange(mockVideoTileController)

        testVideoClientObserver.didReceiveFrame(
            mockVideoClient,
            mockMediaVideoFrame,
            testProfileId,
            testVideoId,
            VideoClient.VIDEO_CLIENT_NO_PAUSE,
            testVideoId
        )

        verify(exactly = 0) { mockVideoTileController.onReceiveFrame(any(), any(), any(), any()) }
    }

    @Test
    fun `onDataMessageReceived should notify observers that subscribe to the topic`() {
        testVideoClientObserver.subscribeToReceiveDataMessage(topic, mockDataMessageObserver)
        testVideoClientObserver.subscribeToReceiveDataMessage(anotherTopic, mockAnotherDataMessageObserver)

        testVideoClientObserver.onDataMessageReceived(arrayOf(dataMessageWithTopic))

        verify(exactly = 1) { mockDataMessageObserver.onDataMessageReceived(any()) }
    }

    @Test
    fun `onDataMessageReceived should notify all observers that subscribe to the topic`() {
        testVideoClientObserver.subscribeToReceiveDataMessage(topic, mockDataMessageObserver)
        testVideoClientObserver.subscribeToReceiveDataMessage(topic, mockAnotherDataMessageObserver)

        testVideoClientObserver.onDataMessageReceived(arrayOf(dataMessageWithTopic))

        verify(exactly = 1) { mockDataMessageObserver.onDataMessageReceived(any()) }
        verify(exactly = 1) { mockAnotherDataMessageObserver.onDataMessageReceived(any()) }
    }

    @Test
    fun `onDataMessageReceived should notify observers for the following message when the first message's topic is not subscribed to`() {
        testVideoClientObserver.subscribeToReceiveDataMessage(topic, mockDataMessageObserver)

        testVideoClientObserver.onDataMessageReceived(arrayOf(dataMessageWithAnotherTopic, dataMessageWithTopic))

        verify(exactly = 1) { mockDataMessageObserver.onDataMessageReceived(any()) }
    }

    @Test
    fun `onTurnURIsReceived should call urlRewriter for each uri passed in`() {
        every { mockDefaultUrlRewriter(uri1) } returns uri1
        every { mockDefaultUrlRewriter(uri2) } returns uri2
        every { mockDefaultUrlRewriter(uri3) } returns uri3
        val outUris = testVideoClientObserver.onTurnURIsReceived(turnUris)

        verify(exactly = 3) { mockDefaultUrlRewriter(any()) }
        assert(outUris.equals(turnUris))
    }
}
