/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate

/**
 * [RealtimeObserver] lets one listen to real time events such a volume, signal strength, or
 * attendee changes.
 *
 * Note: all callbacks will be called on main thread.
 */
interface RealtimeObserver {
    /**
     * Handles volume changes for attendees whose [VolumeLevel] has changed.
     *
     * Note: this callback will be called on main thread.
     *
     * @param volumeUpdates: Array<[VolumeUpdate]> - Attendees with updated volume levels.
     */
    fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>)

    /**
     * Handles signal strength changes for attendees whose [SignalStrength] has changed.
     *
     * Note: this callback will be called on main thread.
     *
     * @param signalUpdates: Array<[SignalUpdate]> - Attendees with updated signal strengths.
     */
    fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>)

    /**
     * Handles attendee(s) being added.
     *
     * Note: this callback will be called on main thread.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees being added.
     */
    fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) being removed.
     *
     * Note: this callback will be called on main thread.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees being removed.
     */
    fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) being dropped due to network.
     *
     * Note: this callback will be called on main thread.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees being dropped.
     */
    fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed to muted.
     *
     * Note: this callback will be called on main thread.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees who are newly muted.
     */
    fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Handles attendee(s) whose [VolumeLevel] has changed from muted.
     *
     * Note: this callback will be called on main thread.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - Attendees who are newly unmuted.
     */
    fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>)
}
