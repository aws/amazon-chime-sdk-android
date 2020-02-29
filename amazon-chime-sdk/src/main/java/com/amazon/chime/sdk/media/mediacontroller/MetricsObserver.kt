/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.enums.ObservableMetric

interface MetricsObserver {
    /**
     * Called when metrics are ready.
     *
     * @param metrics: Map<ObservableMetric, Any> - Map of metric type to value
     */
    fun onMetricsReceive(metrics: Map<ObservableMetric, Any>)
}
