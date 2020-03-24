/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.metric

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.ObservableMetric
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoClient
import java.util.Calendar

/**
 * [DefaultClientMetricsCollector]'s filters and caches incoming raw client metrics
 * and calls the metrics observer with the current values every second
 */
class DefaultClientMetricsCollector :
    ClientMetricsCollector {
    private var metricsObservers = mutableSetOf<MetricsObserver>()
    private var cachedObservableMetrics = mutableMapOf<ObservableMetric, Double?>()
    private var lastEmittedMetricsTime = Calendar.getInstance().timeInMillis
    private val METRICS_EMISSION_INTERVAL_MS = 1000

    override fun processAudioClientMetrics(metrics: Map<Int, Double>) {
        cachedObservableMetrics[ObservableMetric.audioReceivePacketLossPercent] =
            metrics[AudioClient.AUDIO_CLIENT_METRIC_POST_JB_SPK_1S_PACKETS_LOST_PERCENT]
        cachedObservableMetrics[ObservableMetric.audioSendPacketLossPercent] =
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
        cachedObservableMetrics[ObservableMetric.videoSendPacketLossPercent] =
            metrics[VideoClient.VIDEO_SEND_PACKET_LOSS_PERCENT]
        cachedObservableMetrics[ObservableMetric.videoSendFps] =
            metrics[VideoClient.VIDEO_SEND_FPS]
        cachedObservableMetrics[ObservableMetric.videoSendRttMs] =
            metrics[VideoClient.VIDEO_SEND_RTT]
        cachedObservableMetrics[ObservableMetric.videoReceiveBitrate] =
            metrics[VideoClient.VIDEO_RECEIVE_BITRATE]
        cachedObservableMetrics[ObservableMetric.videoReceivePacketLossPercent] =
            metrics[VideoClient.VIDEO_RECEIVE_PACKET_LOSS_PERCENT]
        maybeEmitMetrics()
    }

    private fun maybeEmitMetrics() {
        val now = Calendar.getInstance().timeInMillis
        if (cachedObservableMetrics.isNotEmpty() && now - lastEmittedMetricsTime > METRICS_EMISSION_INTERVAL_MS) {
            lastEmittedMetricsTime = now
            var cachedObservableMetricsWithoutNullValues =
                cachedObservableMetrics.filterValues { it != null } as Map<ObservableMetric, Any>
            metricsObservers.forEach { it.onMetricsReceived(cachedObservableMetricsWithoutNullValues) }
        }
    }

    override fun subscribeToMetrics(observer: MetricsObserver) {
        this.metricsObservers.add(observer)
    }

    override fun unsubscribeFromMetrics(observer: MetricsObserver) {
        this.metricsObservers.remove(observer)
    }
}
