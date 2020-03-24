/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate

/**
 * [RealtimeObserver] lets one listen to real time events such a volume or signal strength changes
 */
interface RealtimeObserver {
    /**
     * Handles volume changes for attendees whose [VolumeLevel] has changed
     *
     * @param volumeUpdates: Array<[VolumeUpdate]> - Attendees with updated volume levels
     */
    fun onVolumeChange(volumeUpdates: Array<VolumeUpdate>)

    /**
     * Handles signal strength changes for attendees whose [SignalStrength] has changed
     *
     * @param signalUpdates: Array<[SignalUpdate]> - Attendees with updated signal strengths
     */
    fun onSignalStrengthChange(signalUpdates: Array<SignalUpdate>)

    /**
     * Handles attendee(s) being added
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees being added
     */
    fun onAttendeesJoin(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) being removed
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees being removed
     */
    fun onAttendeesLeave(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed to muted
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees who are newly muted
     */
    fun onAttendeesMute(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed from muted
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees who are newly unmuted
     */
    fun onAttendeesUnmute(attendeeInfo: Array<AttendeeInfo>)
}
