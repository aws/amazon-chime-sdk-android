package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController

class DefaultRealtimeController(private val audioClientController: AudioClientController) :
    RealtimeControllerFacade {

    override fun realtimeLocalMute(): Boolean {
        return audioClientController.setMute(true)
    }

    override fun realtimeLocalUnmute(): Boolean {
        return audioClientController.setMute(false)
    }

    override fun realtimeAddObserver(observer: RealtimeObserver) {
        audioClientController.subscribeToRealTimeEvents(observer)
    }

    override fun realtimeRemoveObserver(observer: RealtimeObserver) {
        audioClientController.unsubscribeFromRealTimeEvents(observer)
    }
}
