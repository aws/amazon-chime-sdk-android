/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatusCode
import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.DefaultContentShareVideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.session.URLRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.VideoClientFailedError
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import com.xodee.client.video.VideoClientEvent
import com.xodee.client.video.VideoClientSignalingDroppedError
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Skipping all tests due to issue with mocking System::class")
class DefaultContentShareVideoClientObserverTest {
    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockTurnRequestParams: TURNRequestParams

    @MockK
    private lateinit var mockClientMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var mockURLRewriter: URLRewriter

    @MockK
    private lateinit var mockEventAnalyticsController: EventAnalyticsController

    @InjectMockKs
    private lateinit var testContentShareVideoClientObserver: DefaultContentShareVideoClientObserver

    @MockK
    private lateinit var mockContentShareObserver: ContentShareObserver

    @MockK
    private lateinit var mockVideoClient: VideoClient

    private val testDispatcher = TestCoroutineDispatcher()

    private val videoClientStatus = 6

    @Before
    fun setUp() {
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } just runs
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        testContentShareVideoClientObserver.subscribeToVideoClientStateChange(
            mockContentShareObserver
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `didConnect should call publishEvent with ContentShareStarted`() {
        testContentShareVideoClientObserver.didStop(mockVideoClient)

        verify(exactly = 1) {
            mockContentShareObserver.onContentShareStarted()
            mockEventAnalyticsController.publishEvent(EventName.contentShareStarted)
        }
    }

    @Test
    fun `didFail should notify observer of onContentShareStopped event with VideoServiceFailed status`() {
        testContentShareVideoClientObserver.didFail(
            mockVideoClient,
            videoClientStatus,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY
        )

        verify(exactly = 1) {
            mockContentShareObserver.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.VideoServiceFailed
                )
            )
            mockEventAnalyticsController.publishEvent(EventName.contentShareFailed, mutableMapOf(
                EventAttributeName.contentShareErrorMessage to VideoClientFailedError.fromVideoClientStatus(videoClientStatus)
            ))
        }
    }

    @Test
    fun `didFail should call clientMetricsCollector to clean metrics`() {
        testContentShareVideoClientObserver.didFail(
            mockVideoClient,
            videoClientStatus,
            VideoClient.VIDEO_CLIENT_STATUS_CALL_AT_CAPACITY
        )

        verify { mockClientMetricsCollector.processContentShareVideoClientMetrics(emptyMap()) }
    }

    @Test
    fun `didStop should notify observer of onContentShareStopped event with OK status`() {
        testContentShareVideoClientObserver.didStop(mockVideoClient)

        verify(exactly = 1) {
            mockContentShareObserver.onContentShareStopped(
                ContentShareStatus(
                    ContentShareStatusCode.OK
                )
            )
        }
    }

    @Test
    fun `didStop should call clientMetricsCollector to clean metrics`() {
        testContentShareVideoClientObserver.didStop(mockVideoClient)

        verify { mockClientMetricsCollector.processContentShareVideoClientMetrics(emptyMap()) }
        verify(exactly = 1) {
            mockEventAnalyticsController.publishEvent(EventName.contentShareStopped)
        }
    }

    @Test
    fun `onMetrics should call clientMetricsCollector to process video metrics`() {
        testContentShareVideoClientObserver.onMetrics(intArrayOf(1), doubleArrayOf(1.0))

        verify { mockClientMetricsCollector.processContentShareVideoClientMetrics(any()) }
    }

    @Test
    fun `onTurnURIsReceived should call urlRewriter for each uri passed in`() {
        val uri1 = "one"
        val uri2 = "two"
        val uri3 = "three"
        every { mockURLRewriter(uri1) } returns uri1
        every { mockURLRewriter(uri2) } returns uri2
        every { mockURLRewriter(uri3) } returns uri3
        val turnUris = listOf<String>(uri1, uri2, uri3)
        val outUris = testContentShareVideoClientObserver.onTurnURIsReceived(turnUris)

        verify(exactly = 3) { mockURLRewriter(any()) }
        assert(outUris.equals(turnUris))
    }

    @Test
    fun `didReceiveEvent should publish event when event type is sigaling opened`() {
        val event = VideoClientEvent.signalingOpenedEvent(1, 1)
        testContentShareVideoClientObserver.didReceiveEvent(mockVideoClient, event)

        verify {
            mockEventAnalyticsController.publishEvent(
                EventName.videoClientSignalingOpened,
                any()
            )
        }
    }

    @Test
    fun `didReceiveEvent should publish event when event type is sigaling dropped`() {
        val event = VideoClientEvent.signalingDroppedEvent(1, VideoClientSignalingDroppedError.SIGNALING_CLIENT_EOF)
        testContentShareVideoClientObserver.didReceiveEvent(mockVideoClient, event)

        verify {
            mockEventAnalyticsController.publishEvent(
                EventName.videoClientSignalingDropped,
                any()
            )
        }
    }

    @Test
    fun `didReceiveEvent should publish event when event type is ice gathering completed`() {
        val event = VideoClientEvent.iceGatheringCompletedEvent(1, 1)
        testContentShareVideoClientObserver.didReceiveEvent(mockVideoClient, event)

        verify {
            mockEventAnalyticsController.publishEvent(
                EventName.videoClientIceGatheringCompleted,
                any()
            )
        }
    }
}
