/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.enums

/**
 * [ObservableMetric]'s represents filtered metrics that are intended to propagate to the
 * top level observers. All metrics are measured over the past second.
 */
enum class ObservableMetric() {
    // Percentage of audio packets lost from server to client
    audioPacketsReceivedFractionLossPercent,
    // Percentage of audio packets lost from client to server
    audioPacketsSentFractionLossPercent,
    // Estimated uplink bandwidth (may not all be used) from perspective of video client
    videoAvailableSendBandwidth,
    // Estimated downlink bandwidth (may not all be used) from perspective of video client
    videoAvailableReceiveBandwidth,
    // Total bitrate summed accross all send streams
    videoSendBitrate,
    // Total packet lost calculated across all send streams
    videoSendPacketLostPercent,
    // Average send FPS across possibly multiple simulcast streams
    videoSendFps,
    // Total bitrate summed across all receive streams
    videoReceiveBitrate,
    // Total packet lost calculated across all receive streams
    videoReceivePacketLostPercent,
}
