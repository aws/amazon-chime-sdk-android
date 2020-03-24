/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

/**
 * [VideoClientStateController] holds the current [VideoClientState] of the Video Client and
 * handles lifecycle (start, initialize, stop, destroy) for the Video Client.
 * [VideoClientController] should also ask [VideoClientStateController] if an action is allowed
 * based on the current Video Client state.
 */
interface VideoClientStateController {
    /**
     * Update the current Video client state.
     *
     * @param newState: [VideoClientState] - The new video client state.
     */
    fun updateState(newState: VideoClientState)

    /**
     * Bind a handler to handle lifecycle events triggered by Video client state changes.
     *
     * @param lifecycleHandler: [VideoClientLifecycleHandler] - Handler for video client lifecycle events.
     */
    fun bindLifecycleHandler(lifecycleHandler: VideoClientLifecycleHandler)

    /**
     * Moves the video client state to start states. May trigger lifecycle events.
     */
    fun start()

    /**
     * Moves the video client state to stop states. May trigger lifecycle events.
     */
    fun stop()

    /**
     * Check the current video client state against the minimum required state to determine if the
     * action is permitted.
     *
     * @param minimalRequiredState: [VideoClientState] - Minimal required state to allow action.
     * @return Boolean - Whether or not the action is permitted.
     */
    fun canAct(minimalRequiredState: VideoClientState): Boolean
}
