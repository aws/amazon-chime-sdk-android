/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver

/**
 * [ClientMetricsCollector]'s responsibility is to take raw metrics from both
 * the native audio and native video client and consolidate them as needed
 */
interface ClientMetricsCollector {
    /**
     * Collect the raw audio client metrics and filter observable metrics for eventual
     * callback to the observer
     *
     * @param metrics: Map<Int, Double> - Map of raw audio client metric to value
     */
    fun processAudioClientMetrics(metrics: Map<Int, Double>)

    /**
     * Collect the raw video client metrics and filter observable metrics for eventual
     * callback to the observer
     *
     * @param metrics: Map<Int, Double> - Map of raw video client metric to value
     */
    fun processVideoClientMetrics(metrics: Map<Int, Double>)

    /**
     * Subscribe to metric events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to subscribe to metrics with.
     */
    fun subscribeToMetrics(observer: AudioVideoObserver)

    /**
     * Unsubscribe from metric events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to unsubscribe from metrics.
     */
    fun unsubscribeFromMetrics(observer: AudioVideoObserver)
}
