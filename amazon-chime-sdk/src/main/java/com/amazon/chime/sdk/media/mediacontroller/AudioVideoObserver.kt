/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.enums.ObservableMetric
import com.amazon.chime.sdk.session.MeetingSessionStatus

interface AudioVideoObserver {
    /**
     * Called when the session is connecting or reconnecting.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioVideoStartConnecting(reconnecting: Boolean)

    /**
     * Called when the session has started.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioVideoStart(reconnecting: Boolean)

    /**
     * Called when the session has stopped from a started state with the reason
     * provided in the status.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onAudioVideoStop(sessionStatus: MeetingSessionStatus)

    /**
     * Called when reconnection is canceled.
     */
    fun onAudioReconnectionCancel()

    /**
     * Called when the connection health is recovered.
     */
    fun onConnectionRecovered()

    /**
     * Called when connection is becoming poor.
     */
    fun onConnectionBecamePoor()

    /**
     * Called when metrics are ready.
     *
     * @param metrics: Map<ObservableMetric, Any> - Map of metric type to value
     */
    fun onMetricsReceive(metrics: Map<ObservableMetric, Any>)
}
