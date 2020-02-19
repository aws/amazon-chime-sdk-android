package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver

/**
 * [ClientMetricsCollector]'s responsibility is to take raw metrics from both
 * the native audio and native video client and consolidate them as needed
 */
interface ClientMetricsCollector {
    fun processAudioClientMetrics(metrics: Map<Int, Double>)
    fun addObserver(observer: AudioVideoObserver)
    fun removeObserver(observer: AudioVideoObserver)
}
