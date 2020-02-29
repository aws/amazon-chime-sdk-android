/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

interface RealtimeControllerFacade {

    /**
     * Mute the audio input.
     *
     * @return Boolean whether the mute action succeeded
     */
    fun realtimeLocalMute(): Boolean

    /**
     * Unmutes the audio input.
     *
     * @return Boolean whether the unmute action succeeded
     */
    fun realtimeLocalUnmute(): Boolean

    /**
     * Subscribes to real time events with an observer
     *
     * @param observer: [RealtimeObserver] - Observer that handles real time events
     */
    fun addRealtimeObserver(observer: RealtimeObserver)

    /**
     * Unsubscribes from real time events by removing the specified observer
     *
     * @param observer: [RealtimeObserver] - Observer that handles real time events
     */
    fun removeRealtimeObserver(observer: RealtimeObserver)
}
