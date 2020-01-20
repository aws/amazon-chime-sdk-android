package com.amazon.chime.sdk.media.mediacontroller

interface AudioVideoControllerFacade {
    fun addObserver(observer: AudioVideoObserver)
    fun removeObserver(observer: AudioVideoObserver)
    fun start()
    fun stop()
}
