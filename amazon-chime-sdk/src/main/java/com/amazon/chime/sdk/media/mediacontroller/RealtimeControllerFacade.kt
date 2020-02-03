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
     * Subscribes to volume indicator changes with a callback.
     *
     * @param (Map<String, Int>) -> Unit callback processing a map of attendee Ids to volume
     */
    fun realtimeSubscribeToVolumeIndicator(callback: (Map<String, Int>) -> Unit)

    /**
     * Unsubscribes from volume indicator changes by removing the specified callback.
     *
     * @param (Map<String, Int>) -> Unit callback processing a map of attendee Ids to volume
     */
    fun realtimeUnsubscribeFromVolumeIndicator(callback: (Map<String, Int>) -> Unit)

    /**
     * Subscribes from signal strength changes with a callback.
     *
     * @param (Map<String, Int>) -> Unit callback processing a map of attendee Ids to signal strength
     */
    fun realtimeSubscribeToSignalStrengthChange(callback: (Map<String, Int>) -> Unit)

    /**
     * Unsubscribes from signal strength changes by removing the specified callback.
     *
     * @param (Map<String, Int>) -> Unit callback processing a map of attendee Ids to signal strength
     */
    fun realtimeUnsubscribeFromSignalStrengthChange(callback: (Map<String, Int>) -> Unit)
}
