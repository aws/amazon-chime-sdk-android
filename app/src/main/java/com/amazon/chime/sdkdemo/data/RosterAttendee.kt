package com.amazon.chime.sdkdemo.data

import com.amazon.chime.sdk.media.enums.VolumeLevel

data class RosterAttendee(
    val attendeeName: String,
    val volumeLevel: VolumeLevel = VolumeLevel.NotSpeaking
)
