/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus

/**
 * [AudioVideoObserver] handles audio / video session events.
 */
interface AudioVideoObserver {
    /**
     * Called when the audio session is connecting or reconnecting.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioSessionStartedConnecting(reconnecting: Boolean)

    /**
     * Called when the audio session has started.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioSessionStarted(reconnecting: Boolean)

    /**
     * Called when the audio session has stopped from a started state with the reason
     * provided in the status.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus)

    /**
     * Called when audio session cancelled reconnecting.
     */
    fun onAudioSessionCancelledReconnect()

    /**
     * Called when the connection health is recovered.
     */
    fun onConnectionRecovered()

    /**
     * Called when connection became poor.
     */
    fun onConnectionBecamePoor()

    /**
     * Called when the video session is connecting or reconnecting.
     */
    fun onVideoSessionStartedConnecting()

    /**
     * Called when the video session has started. Sometimes there is a non fatal error such as
     * trying to send local video when the capacity was already reached. However, user can still
     * receive remote video in the existing video session.
     *
     * @param sessionStatus: [MeetingSessionStatus] - Additional details on how the video session started.
     */
    fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus)

    /**
     * Called when the video session has stopped from a started state with the reason
     * provided in the status.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus)
}
