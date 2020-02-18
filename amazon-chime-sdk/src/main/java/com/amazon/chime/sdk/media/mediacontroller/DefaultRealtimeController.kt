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

    override fun realtimeAddObserver(observer: RealtimeObserver) {
        audioClientObserver.subscribeToRealTimeEvents(observer)
    }

    override fun realtimeRemoveObserver(observer: RealtimeObserver) {
        audioClientObserver.unsubscribeFromRealTimeEvents(observer)
    }
}
