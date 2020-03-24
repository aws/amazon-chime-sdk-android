/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector

import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.ActiveSpeakerPolicy

/**
 * [ActiveSpeakerDetectorFacade] provides API calls to add and remove
 * active speaker observers.
 */

interface ActiveSpeakerDetectorFacade {

    /**
     * Adds an active speaker observer along with a policy to calculate active speaker scores
     *
     * @param policy: [ActiveSpeakerPolicy]  - Lets you specify a policy to calculate active speaker scores
     * @param observer: [ActiveSpeakerObserver] - Lets you specify an observer to active speaker updates
     */
    fun addActiveSpeakerObserver(
        policy: ActiveSpeakerPolicy,
        observer: ActiveSpeakerObserver
    )

    /**
     * Removes an active speaker observer.
     *
     * @param observer: [ActiveSpeakerObserver] - Lets you remove an observer from active speaker updates
     */
    fun removeActiveSpeakerObserver(observer: ActiveSpeakerObserver)
}
