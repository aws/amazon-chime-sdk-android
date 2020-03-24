/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.metric

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.ObservableMetric
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.Calendar
import org.junit.Before
import org.junit.Test

class DefaultClientMetricsCollectorTest {

    private lateinit var clientMetricsCollector: DefaultClientMetricsCollector

    @MockK
    private lateinit var mockMetricsObserver: MetricsObserver

    // See https://github.com/mockk/mockk/issues/98
    private lateinit var calendar: Calendar

    @Before
    fun setup() {
        calendar = mockk()
        every { calendar.timeInMillis } returnsMany listOf<Long>(0, 1100)
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        clientMetricsCollector =
            DefaultClientMetricsCollector()
    }

    @Test
    fun `onMetrics should not call observer before interval has passed`() {
        every { calendar.timeInMillis } returns 0
        val rawMetrics =
            mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT to 1.0)
        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        verify(exactly = 0) { mockMetricsObserver.onMetricsReceived(any()) }
    }

    @Test
    fun `onMetrics for audio should call observer after interval has passed and observer should not receive any null metrics`() {
        clientMetricsCollector.subscribeToMetrics(mockMetricsObserver)
        val rawMetrics =
            mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT to 1.0)
        val observableMetrics =
            mutableMapOf(ObservableMetric.audioReceivePacketLossPercent to 1.0)

        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        verify(exactly = 1) { mockMetricsObserver.onMetricsReceived(observableMetrics) }
    }

    @Test
    fun `onMetrics for video should call observer after interval has passed and observer should not receive any null metrics`() {
        clientMetricsCollector.subscribeToMetrics(mockMetricsObserver)
        val rawMetrics = mutableMapOf(VideoClient.VIDEO_AVAILABLE_RECEIVE_BANDWIDTH to 10.0)
        val observableMetrics =
            mutableMapOf(ObservableMetric.videoAvailableReceiveBandwidth to 10.0)

        clientMetricsCollector.processVideoClientMetrics(rawMetrics)

        verify(exactly = 1) { mockMetricsObserver.onMetricsReceived(observableMetrics) }
    }

    @Test
    fun `onMetrics should not emit non-observable metrics`() {
        clientMetricsCollector.subscribeToMetrics(mockMetricsObserver)
        val rawMetrics =
            mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_MIC_DEVICE_FRAMES_LOST_PERCENT to 1.0)

        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf<ObservableMetric, Double>()
        verify(exactly = 1) { mockMetricsObserver.onMetricsReceived(observableMetrics) }
    }

    @Test
    fun `onMetrics should not emit invalid metrics`() {
        clientMetricsCollector.subscribeToMetrics(mockMetricsObserver)
        val rawMetrics = mutableMapOf(999 to 1.0)

        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf<ObservableMetric, Double>()
        verify(exactly = 1) { mockMetricsObserver.onMetricsReceived(observableMetrics) }
    }

    @Test
    fun `onMetrics should not notify observer when unsubscribed`() {
        Thread.sleep(1100)
        clientMetricsCollector.subscribeToMetrics(mockMetricsObserver)
        clientMetricsCollector.unsubscribeFromMetrics(mockMetricsObserver)
        val rawMetrics =
            mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT to 1.0)

        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        verify(exactly = 0) { mockMetricsObserver.onMetricsReceived(any()) }
    }
}
