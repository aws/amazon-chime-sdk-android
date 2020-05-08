/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus

/**
 * [AudioVideoObserver] handles audio / video session events.
 *
 * Note: all callbacks will be called on main thread.
 */
interface AudioVideoObserver {
    /**
     * Called when the audio session is connecting or reconnecting.
     *
     * Note: this callback will be called on main thread.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioSessionStartedConnecting(reconnecting: Boolean)

    /**
     * Called when the audio session has started.
     *
     * Note: this callback will be called on main thread.
     *
     * @param reconnecting: Boolean - Whether the session is reconnecting or not.
     */
    fun onAudioSessionStarted(reconnecting: Boolean)

    /**
     * Called when audio session got dropped due to poor network conditions.
     * There will be an automatic attempt of reconnecting it.
     * If the reconnection is successful, [onAudioSessionStarted] will be called with value of reconnecting as true
     *
     * Note: this callback will be called on main thread.
     */
    fun onAudioSessionDropped()

    /**
     * Called when the audio session has stopped with the reason
     * provided in the status. This callback implies that audio client has stopped permanently for this session and there will be
     * no attempt of reconnecting it.
     *
     * Note: this callback will be called on main thread.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus)

    /**
     * Called when audio session cancelled reconnecting.
     *
     * Note: this callback will be called on main thread.
     */
    fun onAudioSessionCancelledReconnect()

    /**
     * Called when the connection health is recovered.
     *
     * Note: this callback will be called on main thread.
     */
    fun onConnectionRecovered()

    /**
     * Called when connection became poor.
     *
     * Note: this callback will be called on main thread.
     */
    fun onConnectionBecamePoor()

    /**
     * Called when the video session is connecting or reconnecting.
     *
     * Note: this callback will be called on main thread.
     */
    fun onVideoSessionStartedConnecting()

    /**
     * Called when the video session has started. Sometimes there is a non fatal error such as
     * trying to send local video when the capacity was already reached. However, user can still
     * receive remote video in the existing video session.
     *
     * Note: this callback will be called on main thread.
     *
     * @param sessionStatus: [MeetingSessionStatus] - Additional details on how the video session started.
     */
    fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus)

    /**
     * Called when the video session has stopped from a started state with the reason
     * provided in the status.
     *
     * Note: this callback will be called on main thread.
     *
     * @param sessionStatus: [MeetingSessionStatus] - The reason why the session has stopped.
     */
    fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus)
}
