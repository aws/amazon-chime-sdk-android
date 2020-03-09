package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel

data class AttendeeInfo(val attendeeId: String, val externalUserId: String)

data class VolumeUpdate(val attendeeInfo: AttendeeInfo, val volumeLevel: VolumeLevel)

data class SignalUpdate(val attendeeInfo: AttendeeInfo, val signalStrength: SignalStrength)
