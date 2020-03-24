/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo

/**
 * [ActiveSpeakerObserver] handles active speaker detection and score changes for attendees.
 */
interface ActiveSpeakerObserver {
    /**
     * @property scoreCallbackIntervalMs: Int? Specifies period (in milliseconds) of updates for
     * onActiveSpeakerScoreChange. If this is null, the observer will not get active
     * speaker score updates. Should be a value greater than 0.
     */
    val scoreCallbackIntervalMs: Int?

    /**
     * Notifies observers of changes to active speaker.
     *
     * @param attendeeInfo: Array<[AttendeeInfo]> - An array of active speakers.
     */
    fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>)

    /**
     * Periodically sends active speaker scores to observers ONLY IF
     * scoreCallbackIntervalMs is not null
     *
     * @param scores: Map<[AttendeeInfo], Double> - Map of active speakers to respective scores.
     * Scores of 0 mean the attendee is not an active speaker.
     */
    fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>)
}
