/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

/**
 * Allows a component to respond to lifecycle events from [VideoClientStateController].
 */
interface VideoClientLifecycleHandler {
    /**
     * Lifecycle event for initializing video client.
     */
    fun initializeVideoClient()

    /**
     * Lifecycle event for starting video client.
     */
    fun startVideoClient()

    /**
     * Lifecycle event for stopping video client.
     */
    fun stopVideoClient()

    /**
     * Lifecycle event for destroying video client.
     */
    fun destroyVideoClient()
}
