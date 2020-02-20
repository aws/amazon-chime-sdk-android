package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.xodee.client.audio.audioclient.AudioClient

/**
 * [DefaultClientMetricsCollector]'s filters and caches incoming raw client metrics
 * and calls the metrics observer with the current values every second
 */
class DefaultClientMetricsCollector() : ClientMetricsCollector {
    private var metricsObservers = mutableSetOf<AudioVideoObserver>()
    private var cachedObservableMetrics = mutableMapOf<ObservableMetric, Double>()
    private var lastEmittedMetricsTime = System.currentTimeMillis()
    private val METRICS_EMISSION_INTERVAL_MS = 1000

    override fun processAudioClientMetrics(metrics: Map<Int, Double>) {
        metrics.forEach { (metric, value) ->
            when (metric) {
                AudioClient.AUDIO_SERVER_METRIC_POST_JB_MIC_1S_PACKETS_LOST_PERCENT -> {
                    cachedObservableMetrics[ObservableMetric.audioPacketsSentFractionLoss] =
                        value
                }
                AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT -> {
                    cachedObservableMetrics[ObservableMetric.audioPacketsReceivedFractionLoss] =
                        value
                }
            }
        }

        maybeEmitMetrics()
    }

    private fun maybeEmitMetrics() {
        val now = System.currentTimeMillis()
        if (cachedObservableMetrics.isNotEmpty() && now - lastEmittedMetricsTime > METRICS_EMISSION_INTERVAL_MS) {
            lastEmittedMetricsTime = now
            metricsObservers.forEach { it.onReceiveMetric(cachedObservableMetrics) }
        }
    }

    override fun addObserver(observer: AudioVideoObserver) {
        this.metricsObservers.add(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        this.metricsObservers.remove(observer)
    }
}
