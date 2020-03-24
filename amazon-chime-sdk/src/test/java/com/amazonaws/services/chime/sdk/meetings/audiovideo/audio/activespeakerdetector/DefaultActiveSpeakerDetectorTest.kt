/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.DefaultActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultActiveSpeakerDetectorTest {

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    private lateinit var activeSpeakerObserverWithScore1: ActiveSpeakerObserver

    private lateinit var activeSpeakerObserverWithScore2: ActiveSpeakerObserver

    private lateinit var activeSpeakerObserverWithoutScore: ActiveSpeakerObserver

    private lateinit var activeSpeakerDetector: DefaultActiveSpeakerDetector

    private lateinit var activeSpeakerPolicy: DefaultActiveSpeakerPolicy

    private val testId1 = "aliceId"
    private val testAttendeeInfo1 =
        AttendeeInfo(testId1, testId1)
    private val testVolumeUpdate1 =
        VolumeUpdate(
            testAttendeeInfo1,
            VolumeLevel.High
        )

    private val testId2 = "bobId"
    private val testAttendeeInfo2 =
        AttendeeInfo(testId2, testId2)
    private val testVolumeUpdate2 =
        VolumeUpdate(
            testAttendeeInfo2,
            VolumeLevel.High
        )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        activeSpeakerDetector = spyk(DefaultActiveSpeakerDetector(audioClientObserver))
        activeSpeakerObserverWithScore1 = spyk(object : ActiveSpeakerObserver {
            override val scoreCallbackIntervalMs: Int?
                get() = 200

            override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
            }

            override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
            }
        })
        activeSpeakerObserverWithScore2 = spyk(object : ActiveSpeakerObserver {
            override val scoreCallbackIntervalMs: Int?
                get() = 400

            override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
            }

            override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
            }
        })
        activeSpeakerObserverWithoutScore = spyk(object : ActiveSpeakerObserver {
            override val scoreCallbackIntervalMs: Int?
                get() = null

            override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
            }

            override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
            }
        })
        activeSpeakerPolicy = DefaultActiveSpeakerPolicy()
    }

    @Test
    fun `DefaultActiveSpeakerDetector should not be null`() {
        assertNotNull(activeSpeakerDetector)
    }

    @Test
    fun `DefaultActiveSpeakerDetector should show no active speakers when no volume update`() {
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithoutScore
        )
        activeSpeakerDetector.onAttendeesJoined(arrayOf(testAttendeeInfo1))
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithoutScore)

        verify(exactly = 0) {
            activeSpeakerObserverWithoutScore.onActiveSpeakerDetected(
                arrayOf(
                    testAttendeeInfo1
                )
            )
        }
    }

    @Test
    fun `DefaultActiveSpeakerDetector should show active speaker on volume update`() {
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithoutScore
        )
        activeSpeakerDetector.onAttendeesJoined(arrayOf(testAttendeeInfo1))
        activeSpeakerDetector.onVolumeChanged(arrayOf(testVolumeUpdate1))
        Thread.sleep(300)
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithoutScore)

        verify(exactly = 1) {
            activeSpeakerObserverWithoutScore.onActiveSpeakerDetected(
                arrayOf(
                    testAttendeeInfo1
                )
            )
        }
    }

    @Test
    fun `DefaultActiveSpeakerDetector should show active speakers scores`() {
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithScore1
        )
        activeSpeakerDetector.onAttendeesJoined(arrayOf(testAttendeeInfo1))
        Thread.sleep(500)
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithScore1)

        verify(exactly = 2) {
            activeSpeakerObserverWithScore1.onActiveSpeakerScoreChanged(
                mutableMapOf(testAttendeeInfo1 to 0.0)
            )
        }
    }

    @Test
    fun `DefaultActiveSpeakerDetector should send active speakers scores to each observer`() {
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithScore1
        )
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithScore2
        )
        activeSpeakerDetector.onAttendeesJoined(arrayOf(testAttendeeInfo1))
        Thread.sleep(500)
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithScore1)
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithScore2)

        verify(exactly = 2) {
            activeSpeakerObserverWithScore1.onActiveSpeakerScoreChanged(
                mutableMapOf(testAttendeeInfo1 to 0.0)
            )
        }
        verify(exactly = 1) {
            activeSpeakerObserverWithScore2.onActiveSpeakerScoreChanged(
                mutableMapOf(testAttendeeInfo1 to 0.0)
            )
        }
        verify(exactly = 0) { activeSpeakerObserverWithScore1.onActiveSpeakerDetected(any()) }
        verify(exactly = 0) { activeSpeakerObserverWithScore2.onActiveSpeakerDetected(any()) }
    }

    @Test
    fun `DefaultActiveSpeakerDetector should show multiple active speaker on volume update`() {
        activeSpeakerDetector.addActiveSpeakerObserver(
            activeSpeakerPolicy,
            activeSpeakerObserverWithoutScore
        )
        activeSpeakerDetector.onAttendeesJoined(arrayOf(testAttendeeInfo1, testAttendeeInfo2))
        activeSpeakerDetector.onVolumeChanged(arrayOf(testVolumeUpdate1, testVolumeUpdate2))
        Thread.sleep(300)
        activeSpeakerDetector.removeActiveSpeakerObserver(activeSpeakerObserverWithoutScore)

        verify(exactly = 1) {
            activeSpeakerObserverWithoutScore.onActiveSpeakerDetected(
                arrayOf(
                    testAttendeeInfo1,
                    testAttendeeInfo2
                )
            )
        }
    }
}
