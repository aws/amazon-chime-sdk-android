/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.enums.ObservableMetric
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoClient

/**
 * [DefaultClientMetricsCollector]'s filters and caches incoming raw client metrics
 * and calls the metrics observer with the current values every second
 */
class DefaultClientMetricsCollector() : ClientMetricsCollector {
    private var metricsObservers = mutableSetOf<AudioVideoObserver>()
    private var cachedObservableMetrics = mutableMapOf<ObservableMetric, Double?>()
    private var lastEmittedMetricsTime = System.currentTimeMillis()
    private val METRICS_EMISSION_INTERVAL_MS = 1000

    override fun processAudioClientMetrics(metrics: Map<Int, Double>) {
        cachedObservableMetrics[ObservableMetric.audioPacketsReceivedFractionLossPercent] =
            metrics[AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT]
        cachedObservableMetrics[ObservableMetric.audioPacketsSentFractionLossPercent] =
            metrics[AudioClient.AUDIO_SERVER_METRIC_POST_JB_MIC_1S_PACKETS_LOST_PERCENT]
        maybeEmitMetrics()
    }

    override fun processVideoClientMetrics(metrics: Map<Int, Double>) {
        cachedObservableMetrics[ObservableMetric.videoAvailableSendBandwidth] =
            metrics[VideoClient.VIDEO_AVAILABLE_SEND_BANDWIDTH]
        cachedObservableMetrics[ObservableMetric.videoAvailableReceiveBandwidth] =
            metrics[VideoClient.VIDEO_AVAILABLE_RECEIVE_BANDWIDTH]
        cachedObservableMetrics[ObservableMetric.videoSendBitrate] =
            metrics[VideoClient.VIDEO_SEND_BITRATE]
        cachedObservableMetrics[ObservableMetric.videoSendPacketLostPercent] =
            metrics[VideoClient.VIDEO_SEND_PACKET_LOST_PERCENT]
        cachedObservableMetrics[ObservableMetric.videoSendFps] =
            metrics[VideoClient.VIDEO_SEND_FPS]
        cachedObservableMetrics[ObservableMetric.videoReceiveBitrate] =
            metrics[VideoClient.VIDEO_RECEIVE_BITRATE]
        cachedObservableMetrics[ObservableMetric.videoReceivePacketLostPercent] =
            metrics[VideoClient.VIDEO_RECEIVE_PACKET_LOST_PERCENT]
        maybeEmitMetrics()
    }

    private fun maybeEmitMetrics() {
        val now = System.currentTimeMillis()
        if (cachedObservableMetrics.isNotEmpty() && now - lastEmittedMetricsTime > METRICS_EMISSION_INTERVAL_MS) {
            lastEmittedMetricsTime = now
            var cachedObservableMetricsWithoutNullValues = cachedObservableMetrics.filterValues { it != null } as Map<ObservableMetric, Any>
            metricsObservers.forEach { it.onMetricsReceive(cachedObservableMetricsWithoutNullValues) }
        }
    }

    override fun subscribeToMetrics(observer: AudioVideoObserver) {
        this.metricsObservers.add(observer)
    }

    override fun unsubscribeFromMetrics(observer: AudioVideoObserver) {
        this.metricsObservers.remove(observer)
    }
}
