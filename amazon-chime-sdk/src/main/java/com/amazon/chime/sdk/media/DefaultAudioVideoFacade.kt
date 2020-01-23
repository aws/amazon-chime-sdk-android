package com.amazon.chime.sdk.media

import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade

class DefaultAudioVideoFacade(
    private val audioVideoController: AudioVideoControllerFacade,
    private val realtimeController: RealtimeControllerFacade
) :
    AudioVideoFacade {

    override fun addObserver(observer: AudioVideoObserver) {
        audioVideoController.addObserver(observer)
    }

    override fun removeObserver(observer: AudioVideoObserver) {
        audioVideoController.removeObserver(observer)
    }

    override fun start() {
        audioVideoController.start()
    }

    override fun stop() {
        audioVideoController.stop()
    }

    override fun realtimeLocalMute(): Boolean {
        return realtimeController.realtimeLocalMute()
    }

    override fun realtimeLocalUnmute(): Boolean {
        return realtimeController.realtimeLocalUnmute()
    }
}
