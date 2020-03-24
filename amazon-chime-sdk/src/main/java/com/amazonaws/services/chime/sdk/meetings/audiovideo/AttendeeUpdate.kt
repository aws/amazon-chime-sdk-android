/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

data class AttendeeInfo(val attendeeId: String, val externalUserId: String)

data class VolumeUpdate(val attendeeInfo: AttendeeInfo, val volumeLevel: VolumeLevel)

data class SignalUpdate(val attendeeInfo: AttendeeInfo, val signalStrength: SignalStrength)
