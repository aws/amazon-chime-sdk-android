/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.ActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import java.util.Timer
import java.util.TimerTask

/**
 * [DefaultActiveSpeakerDetector] A default implementation of the Active Speaker Detector
 *
 * @param audioClientObserver: [AudioClientObserver] - Needed to subscribe the detector
 * to real time events like volume change, attendee mute and attendee unmute
 */

class DefaultActiveSpeakerDetector(
    val audioClientObserver: AudioClientObserver
) : ActiveSpeakerDetectorFacade,
    RealtimeObserver {
    private var speakerScores = mutableMapOf<AttendeeInfo, Double>()
    private var activeSpeakers = mutableListOf<AttendeeInfo>()
    private var mostRecentUpdateTimestamp = mutableMapOf<AttendeeInfo, Long>()
    private var mostRecentAttendeeVolumes = mutableMapOf<AttendeeInfo, VolumeLevel>()
    private var activeSpeakerObservers = mutableSetOf<ActiveSpeakerObserver>()
    private var observerToPolicy = mutableMapOf<ActiveSpeakerObserver, ActiveSpeakerPolicy>()
    private var observerToScoresTimer = mutableMapOf<ActiveSpeakerObserver, Timer>()
    private var activityTimer = Timer("ScheduleActivityTimer", false)
    private val ACTIVITY_WAIT_INTERVAL_MS: Long = 1000
    private val ACTIVITY_UPDATE_INTERVAL_MS: Long = 200
    private val TAG = "ActiveSpeakerDetector"

    init {
        audioClientObserver.subscribeToRealTimeEvents(this)
    }

    override fun addActiveSpeakerObserver(
        policy: ActiveSpeakerPolicy,
        observer: ActiveSpeakerObserver
    ) {
        val isFirstObserver = activeSpeakerObservers.isEmpty()
        activeSpeakerObservers.add(observer)
        observerToPolicy[observer] = policy
        if (isFirstObserver) {
            startActivityTimer()
        }
        startScoresTimerForObserver(observer)
    }

    override fun removeActiveSpeakerObserver(observer: ActiveSpeakerObserver) {
        activeSpeakerObservers.remove(observer)
        observerToPolicy.remove(observer)
        stopScoresTimerForObserver(observer)
        if (activeSpeakerObservers.isEmpty()) {
            stopActivityTimer()
        }
    }

    private fun startActivityTimer() {
        activityTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    observerToPolicy.forEach { (_, policy) ->
                        speakerScores.forEach { (attendeeInfo, _) ->
                            val lastTimestamp = mostRecentUpdateTimestamp[attendeeInfo] ?: 0
                            if (System.currentTimeMillis() - lastTimestamp > ACTIVITY_WAIT_INTERVAL_MS) {
                                updateScore(
                                    policy,
                                    attendeeInfo,
                                    (mostRecentAttendeeVolumes[attendeeInfo]
                                        ?: VolumeLevel.NotSpeaking)
                                )
                            }
                        }
                    }
                }
            },
            ACTIVITY_UPDATE_INTERVAL_MS,
            ACTIVITY_UPDATE_INTERVAL_MS
        )
    }

    private fun startScoresTimerForObserver(observer: ActiveSpeakerObserver) {
        observer.scoreCallbackIntervalMs?.let {
            val scoresTimer = Timer("ScheduleScoresTimer", false)
            scoresTimer.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        observer.onActiveSpeakerScoreChanged(speakerScores)
                    }
                },
                it.toLong(),
                it.toLong()
            )
            observerToScoresTimer[observer] = scoresTimer
        }
    }

    private fun stopActivityTimer() {
        if (activeSpeakerObservers.isEmpty()) {
            activityTimer.cancel()
        }
    }

    private fun stopScoresTimerForObserver(observer: ActiveSpeakerObserver) {
        observerToScoresTimer[observer]?.let {
            it.cancel()
            observerToScoresTimer.remove(observer)
        }
    }

    private fun updateScore(
        policy: ActiveSpeakerPolicy,
        attendeeInfo: AttendeeInfo,
        volume: VolumeLevel
    ) {
        val activeScore = policy.calculateScore(attendeeInfo, volume)
        if (speakerScores[attendeeInfo] != activeScore) {
            speakerScores[attendeeInfo] = activeScore
            mostRecentUpdateTimestamp[attendeeInfo] = System.currentTimeMillis()
            updateActiveSpeakers(attendeeInfo)
        }
    }

    private fun updateActiveSpeakers(attendeeInfo: AttendeeInfo) {
        if (!shouldUpdateActiveSpeakerList(attendeeInfo)) {
            return
        }
        val sortedSpeakers = speakerScores.filterValues { it != 0.0 }.toList().sortedBy { (_, value) -> value }.map { it.first }
        activeSpeakerObservers.forEach {
            it.onActiveSpeakerDetected(sortedSpeakers.toTypedArray())
        }
        activeSpeakers = sortedSpeakers.toMutableList()
    }

    private fun shouldUpdateActiveSpeakerList(attendeeInfo: AttendeeInfo): Boolean {
        val score = speakerScores[attendeeInfo] ?: 0.0
        return ((score == 0.0 && activeSpeakers.contains(attendeeInfo)) ||
                (score > 0.0 && !activeSpeakers.contains(attendeeInfo)))
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        volumeUpdates.forEach {
            mostRecentAttendeeVolumes[it.attendeeInfo] = it.volumeLevel
        }
    }

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            speakerScores[it] = 0.0
            mostRecentAttendeeVolumes[it] = VolumeLevel.NotSpeaking
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach {
            speakerScores.remove(it)
            mostRecentAttendeeVolumes.remove(it)
            mostRecentUpdateTimestamp.remove(it)
            updateActiveSpeakers(it)
        }
    }

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        // Not needed for active speaker detection as we solely rely on volume levels
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        // Not needed as mute state is propagated in onVolumeChange
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        // Not needed as unmute state can be obtained from onVolumeChange
    }
}
