package com.amazon.chime.sdk.media.mediacontroller

interface RealtimeControllerFacade {
    fun realtimeLocalMute(): Boolean
    fun realtimeLocalUnmute(): Boolean
}
