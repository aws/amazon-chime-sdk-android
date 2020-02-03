package com.amazon.chime.sdk.media.mediacontroller

/**
 * [[RealtimeObserver]] lets one listen to real time events such a volume or signal strength changes
 */
interface RealtimeObserver {
    /**
     * Handles volume changes for attendees
     *
     * @param attendeeVolumes: Map<String, Int> - A map of attendee Ids to volume
     */
    fun onVolumeChange(attendeeVolumes: Map<String, Int>)

    /**
     * Handles signal strength changes for attendees
     *
     * @param attendeeVolumes: Map<String, Int> - A map of attendee Ids to signal strength
     */
    fun onSignalStrengthChange(attendeeSignalStrength: Map<String, Int>)
}
