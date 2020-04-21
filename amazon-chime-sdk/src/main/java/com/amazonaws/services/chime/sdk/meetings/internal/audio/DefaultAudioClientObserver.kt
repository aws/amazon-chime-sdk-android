/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.internal.SessionStateControllerAction
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AttendeeUpdate
import com.xodee.client.audio.audioclient.AudioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultAudioClientObserver(
    private val logger: Logger,
    private val clientMetricsCollector: ClientMetricsCollector
) : AudioClientObserver {
    private val TAG = "DefaultAudioClientObserver"

    /**
     * currentAudioState is the current state of the [AudioClient]. For example, it is
     * connecting, or has connected, or is disconnecting. [AudioClient] will return different
     * status codes but we will map it to [SessionStateControllerAction].
     *
     * currentAudioStatus is the reason for the state change. For example, status has changed to
     * disconnected because of an internal server error. [AudioClient] also will return different
     * codes that we map to [MeetingSessionStatusCode].
     */
    private var currentAudioState = SessionStateControllerAction.Init
    private var currentAudioStatus: MeetingSessionStatusCode? = MeetingSessionStatusCode.OK

    private var audioClientStateObservers = mutableSetOf<AudioVideoObserver>()
    private var realtimeEventObservers = mutableSetOf<RealtimeObserver>()

    /**
     * Volume state change can be used to figure out the meeting's current attendees.
     * Keep track of attendees here to determine additions / removals to notify observers
     */
    private var currentAttendees = mutableSetOf<AttendeeInfo>()
    private var currentAttendeeVolumeMap = mapOf<String, VolumeUpdate>()
    private var currentAttendeeSignalMap = mapOf<String, SignalUpdate>()

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onAudioClientStateChange(state: Int, status: Int) {
        uiScope.launch {
            handleAudioClientStateChange(state, status)
        }
    }

    override fun onVolumeStateChange(attendeeUpdates: Array<out AttendeeUpdate>?) {
        if (attendeeUpdates == null) return
        val attendeeUpdatesNonEmptyExternalId =
            attendeeUpdates.filter { it.externalUserId.isNotEmpty() }

        onAttendeesChange(attendeeUpdatesNonEmptyExternalId.map {
            AttendeeInfo(
                it.profileId,
                it.externalUserId
            )
        }.toTypedArray())

        val newAttendeeVolumeMap: Map<String, VolumeUpdate> =
            attendeeUpdatesNonEmptyExternalId.mapNotNull { attendeeUpdate ->
                VolumeLevel.from(attendeeUpdate.data)?.let {
                    val attendeeInfo =
                        AttendeeInfo(
                            attendeeUpdate.profileId,
                            attendeeUpdate.externalUserId
                        )
                    attendeeUpdate.profileId to VolumeUpdate(
                        attendeeInfo,
                        it
                    )
                }
            }.toMap()

        val changedAttendeeVolumeMap: Map<String, VolumeUpdate> =
            newAttendeeVolumeMap.filter { (key, value) ->
                value.volumeLevel != currentAttendeeVolumeMap[key]?.volumeLevel
            }

        onAttendeesMuteStateChange(changedAttendeeVolumeMap)

        if (changedAttendeeVolumeMap.isNotEmpty()) {
            val volumeUpdates: Array<VolumeUpdate> = changedAttendeeVolumeMap.values.toTypedArray()
            realtimeEventObservers.forEach { it.onVolumeChanged(volumeUpdates) }
        }

        currentAttendeeVolumeMap = newAttendeeVolumeMap
    }

    override fun onSignalStrengthChange(attendeeUpdates: Array<out AttendeeUpdate>?) {
        if (attendeeUpdates == null) return

        val newAttendeeSignalMap: Map<String, SignalUpdate> =
            attendeeUpdates.filter { it.externalUserId.isNotEmpty() }
                .mapNotNull { attendeeUpdate ->
                    SignalStrength.from(attendeeUpdate.data)?.let {
                        val attendeeInfo =
                            AttendeeInfo(
                                attendeeUpdate.profileId,
                                attendeeUpdate.externalUserId
                            )
                        attendeeUpdate.profileId to SignalUpdate(
                            attendeeInfo,
                            it
                        )
                    }
                }.toMap()

        val changedAttendeeSignalMap: Map<String, SignalUpdate> =
            newAttendeeSignalMap.filter { (key, value) ->
                value.signalStrength != currentAttendeeSignalMap[key]?.signalStrength
            }

        if (changedAttendeeSignalMap.isNotEmpty()) {
            val signalUpdates: Array<SignalUpdate> = changedAttendeeSignalMap.values.toTypedArray()
            realtimeEventObservers.forEach { it.onSignalStrengthChanged(signalUpdates) }
        }

        currentAttendeeSignalMap = newAttendeeSignalMap
    }

    private fun onAttendeesMuteStateChange(volumesDelta: Map<String, VolumeUpdate>) {
        val mutedAttendeeMap: Map<String, VolumeUpdate> = volumesDelta.filter { (_, value) ->
            value.volumeLevel == VolumeLevel.Muted
        }

        if (mutedAttendeeMap.isNotEmpty()) {
            val mutedAttendeeInfo: Array<AttendeeInfo> =
                mutedAttendeeMap.map { (_, value) -> value.attendeeInfo }.toTypedArray()
            realtimeEventObservers.forEach { it.onAttendeesMuted(mutedAttendeeInfo) }
        }

        val unMutedAttendeeMap = volumesDelta.filter { (key, _) ->
            currentAttendeeVolumeMap[key]?.volumeLevel == VolumeLevel.Muted
        }

        if (unMutedAttendeeMap.isNotEmpty()) {
            val unMutedAttendeeInfo: Array<AttendeeInfo> =
                unMutedAttendeeMap.map { (_, value) -> value.attendeeInfo }.toTypedArray()
            realtimeEventObservers.forEach { it.onAttendeesUnmuted(unMutedAttendeeInfo) }
        }
    }

    private fun onAttendeesChange(attendeeInfo: Array<AttendeeInfo>) {
        val newAttendeeSet: MutableSet<AttendeeInfo> = attendeeInfo.toMutableSet()
        val addedAttendeeSet: Set<AttendeeInfo> = newAttendeeSet.minus(currentAttendees)
        val removedAttendeeSet: Set<AttendeeInfo> = currentAttendees.minus(newAttendeeSet)

        logger.debug(TAG, "Current: $currentAttendees")
        logger.debug(TAG, "Added: $addedAttendeeSet")
        logger.debug(TAG, "Removed: $removedAttendeeSet")
        logger.debug(TAG, "New: $newAttendeeSet")

        if (addedAttendeeSet.isNotEmpty()) {
            val addedAttendeeArray: Array<AttendeeInfo> = addedAttendeeSet.toTypedArray()
            realtimeEventObservers.forEach { it.onAttendeesJoined(addedAttendeeArray) }
        }

        if (removedAttendeeSet.isNotEmpty()) {
            val removedAttendeeArray: Array<AttendeeInfo> = removedAttendeeSet.toTypedArray()
            realtimeEventObservers.forEach { it.onAttendeesLeft(removedAttendeeArray) }
        }

        currentAttendees = newAttendeeSet
    }

    override fun onLogMessage(logLevel: Int, message: String?) {
        if (message == null) return

        // Only print error and fatal as the Media team's request to avoid noise
        // Will be changed back to respect logger settings once sanitize the logs
        if (logLevel == AudioClient.L_ERROR || logLevel == AudioClient.L_FATAL) {
            logger.error(TAG, message)
        }
    }

    override fun onMetrics(metrics: IntArray?, values: DoubleArray?) {
        if (metrics == null || values == null) return

        val metricMap = mutableMapOf<Int, Double>()
        (metrics.indices).map { i -> metricMap[metrics[i]] = values[i] }
        clientMetricsCollector.processAudioClientMetrics(metricMap)
    }

    override fun subscribeToAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.add(observer)
    }

    override fun unsubscribeFromAudioClientStateChange(observer: AudioVideoObserver) {
        this.audioClientStateObservers.remove(observer)
    }

    override fun subscribeToRealTimeEvents(observer: RealtimeObserver) {
        realtimeEventObservers.add(observer)
    }

    override fun unsubscribeFromRealTimeEvents(observer: RealtimeObserver) {
        realtimeEventObservers.remove(observer)
    }

    override fun notifyAudioClientObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        for (observer in audioClientStateObservers) {
            observerFunction(observer)
        }
    }

    private fun toAudioClientState(internalAudioClientState: Int): SessionStateControllerAction {
        return when (internalAudioClientState) {
            AudioClient.AUDIO_CLIENT_STATE_UNKNOWN -> SessionStateControllerAction.Unknown
            AudioClient.AUDIO_CLIENT_STATE_INIT -> SessionStateControllerAction.Init
            AudioClient.AUDIO_CLIENT_STATE_CONNECTING -> SessionStateControllerAction.Connecting
            AudioClient.AUDIO_CLIENT_STATE_CONNECTED -> SessionStateControllerAction.FinishConnecting
            AudioClient.AUDIO_CLIENT_STATE_RECONNECTING -> SessionStateControllerAction.Reconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING -> SessionStateControllerAction.Disconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL -> SessionStateControllerAction.FinishDisconnecting
            AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
            AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP -> SessionStateControllerAction.Fail
            else -> SessionStateControllerAction.Unknown
        }
    }

    private fun toAudioStatus(internalAudioStatus: Int): MeetingSessionStatusCode? {
        return when (internalAudioStatus) {
            AudioClient.AUDIO_CLIENT_OK -> MeetingSessionStatusCode.OK
            AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP -> MeetingSessionStatusCode.NetworkBecamePoor
            AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP -> MeetingSessionStatusCode.AudioDisconnected
            AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE -> MeetingSessionStatusCode.AudioJoinedFromAnotherDevice
            AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR -> MeetingSessionStatusCode.AudioInternalServerError
            AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED -> MeetingSessionStatusCode.AudioAuthenticationRejected
            AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY -> MeetingSessionStatusCode.AudioCallAtCapacity
            AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE -> MeetingSessionStatusCode.AudioServiceUnavailable
            AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO -> MeetingSessionStatusCode.AudioDisconnectAudio
            AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED -> MeetingSessionStatusCode.AudioCallEnded
            else -> null
        }
    }

    private fun handleAudioClientStateChange(
        newInternalAudioState: Int,
        newInternalAudioStatus: Int
    ) {
        val newAudioState: SessionStateControllerAction = toAudioClientState(newInternalAudioState)
        val newAudioStatus: MeetingSessionStatusCode? = toAudioStatus(newInternalAudioStatus)

        if (newAudioState == SessionStateControllerAction.Unknown) return
        if (newAudioState == currentAudioState && newAudioStatus == currentAudioStatus) return

        when (newAudioState) {
            SessionStateControllerAction.FinishConnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting ->
                        notifyAudioClientObserver { observer -> observer.onAudioSessionStarted(false) }
                    SessionStateControllerAction.Reconnecting ->
                        notifyAudioClientObserver { observer -> observer.onAudioSessionStarted(true) }
                    SessionStateControllerAction.FinishConnecting ->
                        when (newAudioStatus) {
                            MeetingSessionStatusCode.OK ->
                                if (currentAudioStatus == MeetingSessionStatusCode.NetworkBecamePoor) {
                                    notifyAudioClientObserver { observer -> observer.onConnectionRecovered() }
                                }
                            MeetingSessionStatusCode.NetworkBecamePoor ->
                                if (currentAudioStatus == MeetingSessionStatusCode.OK) {
                                    notifyAudioClientObserver { observer -> observer.onConnectionBecamePoor() }
                                }
                        }
                }
            }
            SessionStateControllerAction.Reconnecting -> {
                if (currentAudioState == SessionStateControllerAction.FinishConnecting) {
                    notifyAudioClientObserver { observer -> observer.onAudioSessionStarted(true) }
                }
            }
            SessionStateControllerAction.FinishDisconnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting ->
                        notifyAudioClientObserver { observer ->
                            observer.onAudioSessionStopped(
                                MeetingSessionStatus(MeetingSessionStatusCode.OK)
                            )
                        }
                    SessionStateControllerAction.Reconnecting ->
                        notifyAudioClientObserver { observer -> observer.onAudioSessionCancelledReconnect() }
                }
            }
            SessionStateControllerAction.Fail -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting ->
                        notifyAudioClientObserver { observer ->
                            observer.onAudioSessionStopped(MeetingSessionStatus(newAudioStatus))
                        }
                    SessionStateControllerAction.Reconnecting ->
                        notifyAudioClientObserver { observer ->
                            observer.onAudioSessionCancelledReconnect()
                            observer.onAudioSessionStopped(MeetingSessionStatus(newAudioStatus))
                        }
                }
            }
        }
        currentAudioState = newAudioState
        currentAudioStatus = newAudioStatus
    }
}
