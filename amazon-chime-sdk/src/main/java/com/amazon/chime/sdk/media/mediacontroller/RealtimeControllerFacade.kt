package com.amazon.chime.sdk.media.mediacontroller

interface RealtimeControllerFacade {
    fun realtimeLocalMute(): Boolean
    fun realtimeLocalUnmute(): Boolean
    fun realtimeSubscribeToVolumeIndicator(callback: (Map<String, Int>) -> Unit)
    fun realtimeUnsubscribeFromVolumeIndicator(callback: (Map<String, Int>) -> Unit)
    fun realtimeSubscribeToSignalStrengthChange(callback: (Map<String, Int>) -> Unit)
    fun realtimeUnsubscribeFromSignalStrengthChange(callback: (Map<String, Int>) -> Unit)
}
