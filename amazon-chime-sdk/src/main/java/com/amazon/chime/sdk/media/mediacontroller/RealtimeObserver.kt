/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel

/**
 * [[RealtimeObserver]] lets one listen to real time events such a volume or signal strength changes
 */
interface RealtimeObserver {
    /**
     * Handles volume changes for attendees whose [VolumeLevel] has changed
     *
     * @param attendeeVolumes: Map<String, [VolumeLevel]> - A map of attendee Ids to volume
     */
    fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>)

    /**
     * Handles signal strength changes for attendees whose [SignalStrength] has changed
     *
     * @param attendeeSignalStrength: Map<String, [SignalStrength]> - A map of attendee Ids to signal strength
     */
    fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>)

    /**
     * Handles attendee(s) being added
     *
     * @param attendeeIds: Array<String> - The Ids for the attendees being added
     */
    fun onAttendeesJoin(attendeeIds: Array<String>)

    /**
     * Handles attendee(s) being removed
     *
     * @param attendeeIds: Array<String> - The Ids for the attendees being removed
     */
    fun onAttendeesLeave(attendeeIds: Array<String>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed to muted
     *
     * @param attendeeIds: Array<String> - The Ids for the attendees being muted
     */
    fun onAttendeesMute(attendeeIds: Array<String>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed from muted
     *
     * @param attendeeIds: Array<String> - The Ids for the attendees being unmuted
     */
    fun onAttendeesUnmute(attendeeIds: Array<String>)
}
