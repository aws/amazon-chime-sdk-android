/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel

/**
 * [ActiveSpeakerPolicy] calculates a normalized score of how active a speaker is. Implementations
 * of [ActiveSpeakerPolicy] provide custom algorithms for calculating the score.
 */
interface ActiveSpeakerPolicy {
    /**
     * Return the score of the speaker. If the score is 0, this speaker is not active.
     *
     * @param attendeeInfo: [AttendeeInfo] - Attendee information containing attendeeId and
     * externalUserId
     * @param volume: [VolumeLevel] - Current volume of attendee
     */
    fun calculateScore(attendeeInfo: AttendeeInfo, volume: VolumeLevel): Double
}
