/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.xodee.client.video.VideoClientDelegate
import com.xodee.client.video.VideoClientLogListener

/**
 * [VideoClientObserver] handles all callbacks related to the Video Client and allows higher level
 * components to observe the lower level Video Client events.
 */
interface VideoClientObserver : VideoClientDelegate, VideoClientLogListener {
    /**
     * Subscribe to audio, video, and connection events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to subscribe to events with.
     */
    fun subscribeToVideoClientStateChange(observer: AudioVideoObserver)

    /**
     * Unsubscribe from audio, video, and connection events by removing specified [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to unsubscribe from events with.
     */
    fun unsubscribeFromVideoClientStateChange(observer: AudioVideoObserver)

    /**
     * Notify existing observers of video tile events to invoke a function
     *
     * @param observerFunction: (observer: [VideoTileController]) -> Unit) - Observer function to invoke.
     */
    fun notifyVideoTileObserver(observerFunction: (observer: VideoTileController) -> Unit)

    /**
     * Subscribe to video tile events with [VideoTileController].
     *
     * @param observer: [VideoTileController] - The observer to subscribe to events with.
     */
    fun subscribeToVideoTileChange(observer: VideoTileController)

    /**
     * Unsubscribe from video tile events with [VideoTileController].
     *
     * @param observer: [VideoTileController] - The observer to unsubscribe from events with.
     */
    fun unsubscribeFromVideoTileChange(observer: VideoTileController)
}
