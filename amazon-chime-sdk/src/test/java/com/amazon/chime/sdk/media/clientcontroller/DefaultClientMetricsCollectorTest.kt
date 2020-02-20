package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultClientMetricsCollectorTest {

    private lateinit var clientMetricsCollector: DefaultClientMetricsCollector

    @MockK
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        clientMetricsCollector = DefaultClientMetricsCollector()

        // TODO: Investigate and implement mocking of passage of time intervals
    }

    @Test
    fun `onMetrics should not call observer before interval has passed`() {
        val rawMetrics = mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT to 1.0)
        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf(ObservableMetric.audioPacketsReceivedFractionLoss to 1.0)
        verify(exactly = 0) { mockAudioVideoObserver.onReceiveMetric(observableMetrics) }
    }

    @Test
    fun `onMetrics should call observer after interval has passed`() {
        Thread.sleep(1100)

        clientMetricsCollector.addObserver(mockAudioVideoObserver)
        val rawMetrics = mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT to 1.0)
        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf(ObservableMetric.audioPacketsReceivedFractionLoss to 1.0)
        verify(exactly = 1) { mockAudioVideoObserver.onReceiveMetric(observableMetrics) }
    }

    @Test
    fun `onMetrics should not emit non-observable metrics`() {
        Thread.sleep(1100)

        clientMetricsCollector.addObserver(mockAudioVideoObserver)
        val rawMetrics = mutableMapOf(AudioClient.AUDIO_CLIENT_METRIC_MIC_DEVICE_FRAMES_LOST_PERCENT to 1.0)
        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf<ObservableMetric, Double>()
        verify(exactly = 1) { mockAudioVideoObserver.onReceiveMetric(observableMetrics) }
    }

    @Test
    fun `onMetrics should not emit invalid metrics`() {
        Thread.sleep(1100)

        clientMetricsCollector.addObserver(mockAudioVideoObserver)
        val rawMetrics = mutableMapOf(999 to 1.0)
        clientMetricsCollector.processAudioClientMetrics(rawMetrics)

        val observableMetrics = mutableMapOf<ObservableMetric, Double>()
        verify(exactly = 1) { mockAudioVideoObserver.onReceiveMetric(observableMetrics) }
    }
}
