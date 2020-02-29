/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver

class DefaultRealtimeController(
    private val audioClientController: AudioClientController,
    private val audioClientObserver: AudioClientObserver
) :
    RealtimeControllerFacade {

    override fun realtimeLocalMute(): Boolean {
        return audioClientController.setMute(true)
    }

    override fun realtimeLocalUnmute(): Boolean {
        return audioClientController.setMute(false)
    }

    override fun addRealtimeObserver(observer: RealtimeObserver) {
        audioClientObserver.subscribeToRealTimeEvents(observer)
    }

    override fun removeRealtimeObserver(observer: RealtimeObserver) {
        audioClientObserver.unsubscribeFromRealTimeEvents(observer)
    }
}
