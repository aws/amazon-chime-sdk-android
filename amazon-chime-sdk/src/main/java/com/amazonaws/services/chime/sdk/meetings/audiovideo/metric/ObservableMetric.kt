/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.metric

/**
 * [ObservableMetric] represents filtered metrics that are intended to propagate to the
 * top level observers. All metrics are measured over the past second.
 */
enum class ObservableMetric() {
    /**
     * Percentage of audio packets lost from server to client
     */
    audioReceivePacketLossPercent,

    /**
     * Percentage of audio packets lost from client to server
     */
    audioSendPacketLossPercent,

    /**
     * Estimated uplink bandwidth from perspective of video client
     */
    videoAvailableSendBandwidth,

    /**
     * Estimated downlink bandwidth  from perspective of video client
     */
    videoAvailableReceiveBandwidth,

    /**
     * Sum of total bitrate across all send streams
     */
    videoSendBitrate,

    /**
     * Percentage of video packets lost from client to server across all send streams
     */
    videoSendPacketLossPercent,

    /**
     * Average send FPS across all send streams
     */
    videoSendFps,

    /**
     * Round trip time of packets sent from client to server
     */
    videoSendRttMs,

    /**
     * Sum of total bitrate across all receive streams
     */
    videoReceiveBitrate,

    /**
     * Percentage of video packets lost from server to client across all receive streams
     */
    videoReceivePacketLossPercent,
}
