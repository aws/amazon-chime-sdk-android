/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.activespeakerpolicy

import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AttendeeInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultActiveSpeakerPolicyTest {

    private lateinit var activeSpeakerPolicy: DefaultActiveSpeakerPolicy

    private val attendeeInfo = AttendeeInfo("attendeeId", "externalUserId")

    @Before
    fun setup() {
        activeSpeakerPolicy = DefaultActiveSpeakerPolicy()
    }

    @Test
    fun `DefaultActiveSpeakerPolicy should not be null`() {
        assert(activeSpeakerPolicy != null)
    }

    @Test
    fun `speaker score should be 0 when volume is muted`() {
        val score = activeSpeakerPolicy.calculateScore(attendeeInfo, VolumeLevel.Muted)

        assert(score == 0.0)
    }

    @Test
    fun `speaker score should not be 0 when volume is greater than NotSpeaking`() {
        val score = activeSpeakerPolicy.calculateScore(attendeeInfo, VolumeLevel.Low)

        assert(score > 0.0)
    }
}
