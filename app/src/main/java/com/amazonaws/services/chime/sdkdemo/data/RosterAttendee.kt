/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdkdemo.data

import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel

data class RosterAttendee(
    val attendeeId: String,
    val attendeeName: String,
    val volumeLevel: VolumeLevel = VolumeLevel.NotSpeaking,
    val signalStrength: SignalStrength = SignalStrength.High
)
