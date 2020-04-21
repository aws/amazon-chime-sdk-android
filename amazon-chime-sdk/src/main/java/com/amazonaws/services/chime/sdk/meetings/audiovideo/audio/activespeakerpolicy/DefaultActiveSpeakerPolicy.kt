/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import kotlin.math.max

/**
 * [DefaultActiveSpeakerPolicy] A default implementation of the Active Speaker Policy
 */

class DefaultActiveSpeakerPolicy : ActiveSpeakerPolicy {
    /**
     * Map of attendees to their current active speaker scores
     */
    private var speakerScores = mutableMapOf<AttendeeInfo, Double>()

    /**
     * Specifies how much weight we give to the existing score of the attendee
     */
    private val speakerWeight = 0.9

    /**
     * Only scores above this threshold will be considered active
     */
    private val cutoffThreshold = 0.01

    /**
     * This constant is used to reduce the active speaker score of other attendees
     * indicating that the current attendee has "possibly" started speaking because
     * the latest volume received was high
     */
    private val takeoverRate = 0.2

    /**
     * normalizeFactor is used to convert the VolumeLevel from 1-3 scale to 0-1 scale
     */
    private val normalizeFactor = 3

    override fun calculateScore(attendeeInfo: AttendeeInfo, volume: VolumeLevel): Double {
        var normalizedVolume: Double = volume.value.toDouble() / normalizeFactor

        if (!speakerScores.containsKey(attendeeInfo)) {
            speakerScores[attendeeInfo] = 0.0
        }

        if (normalizedVolume > 0.0) {
            normalizedVolume = 1.0
        } else {
            normalizedVolume = 0.0
        }

        val score: Double = (speakerScores.getValue(attendeeInfo) * speakerWeight) + (normalizedVolume * (1.0 - speakerWeight))
        speakerScores[attendeeInfo] = score

        speakerScores.filterKeys { it != attendeeInfo }.forEach { (otherAttendee, currentScore) ->
            val newOtherAttendeeScore = currentScore - (takeoverRate * normalizedVolume)
            speakerScores[otherAttendee] = max(newOtherAttendeeScore, 0.0)
        }

        if (score < this.cutoffThreshold) {
            return 0.0
        }
        return score
    }
}
