/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus

/**
 * [AudioVideoObserver] handles audio/video client events.
 */
interface AudioVideoObserver {
    /**
     * Called when the audio session is connecting or reconnecting.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioClientConnecting(reconnecting: Boolean)

    /**
     * Called when the audio session has started.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioClientStart(reconnecting: Boolean)

    /**
     * Called when the audio session has stopped from a started state with the reason
     * provided in the status.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onAudioClientStop(sessionStatus: MeetingSessionStatus)

    /**
     * Called when audio reconnection is canceled.
     */
    fun onAudioClientReconnectionCancel()

    /**
     * Called when the connection health is recovered.
     */
    fun onConnectionRecover()

    /**
     * Called when connection is becoming poor.
     */
    fun onConnectionBecomePoor()

    /**
     * Called when the video session is connecting or reconnecting.
     */
    fun onVideoClientConnecting()

    /**
     * Called when the video session has started.
     */
    fun onVideoClientStart()

    /**
     * Called when the video session has stopped from a started state with the reason
     * provided in the status.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onVideoClientStop(sessionStatus: MeetingSessionStatus)

    /**
     * Called when an error is encountered in the video session with the reason provided in the status.
     * In this case, the error does not cause the video session to stop. For example, the video client
     * may have tried to send video but found that the capacity was already reached. However, the video
     * client can still receive remote video streams.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onVideoClientError(sessionStatus: MeetingSessionStatus)
}
