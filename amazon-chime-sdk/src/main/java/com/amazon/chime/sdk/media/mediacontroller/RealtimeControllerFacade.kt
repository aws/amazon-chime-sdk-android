package com.amazon.chime.sdk.media.mediacontroller

interface RealtimeControllerFacade {
    fun realtimeLocalMute()
    fun realtimeLocalUnmute(): Boolean
}
