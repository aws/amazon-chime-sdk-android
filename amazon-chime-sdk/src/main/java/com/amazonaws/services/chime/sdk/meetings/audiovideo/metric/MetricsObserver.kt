/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.metric

/**
 * [MetricsObserver] handles events related to audio/video metrics.
 */
interface MetricsObserver {
    /**
     * Called when metrics are ready.
     *
     * @param metrics: Map<ObservableMetric, Any> - Map of metric type to value
     */
    fun onMetricsReceive(metrics: Map<ObservableMetric, Any>)
}
