/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

/**
 * [ObservableMetric]'s represents filtered metrics that are intended to propagate to the
 * top level observers
 */
enum class ObservableMetric() {
    audioPacketsReceivedFractionLoss,
    audioPacketsSentFractionLoss,
}
