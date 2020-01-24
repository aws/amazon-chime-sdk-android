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

    override fun realtimeSubscribeToVolumeIndicator(callback: (Map<String, Int>) -> Unit) {
        audioClientController.subscribeToVolumeIndicator(callback)
    }

    override fun realtimeUnsubscribeFromVolumeIndicator(callback: (Map<String, Int>) -> Unit) {
        audioClientController.unsubscribeFromVolumeIndicator(callback)
    }

    override fun realtimeSubscribeToSignalStrengthChange(callback: (Map<String, Int>) -> Unit) {
        audioClientController.subscribeToSignalStrengthChange(callback)
    }

    override fun realtimeUnsubscribeFromSignalStrengthChange(callback: (Map<String, Int>) -> Unit) {
        audioClientController.unsubscribeFromSignalStrengthChange(callback)
    }
}
