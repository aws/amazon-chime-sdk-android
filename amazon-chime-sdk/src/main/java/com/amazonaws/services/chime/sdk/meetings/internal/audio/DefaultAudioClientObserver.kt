/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.PrimaryMeetingPromotionObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.Transcript
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptAlternative
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptEntity
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptEvent
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptItem
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptItemType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptLanguageWithScore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptResult
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptionStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptionStatusType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.ingestion.AppStateMonitor
import com.amazonaws.services.chime.sdk.meetings.internal.AttendeeStatus
import com.amazonaws.services.chime.sdk.meetings.internal.SessionStateControllerAction
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.TranscriptEventObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AttendeeUpdate
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.transcript.Transcript as TranscriptInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptEvent as TranscriptEventInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptionStatus as TranscriptionStatusInternal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DefaultAudioClientObserver(
    private val logger: Logger,
    private val clientMetricsCollector: ClientMetricsCollector,
    private val configuration: MeetingSessionConfiguration,
    private val meetingStatsCollector: MeetingStatsCollector,
    private val eventAnalyticsController: EventAnalyticsController,
    private val appStateMonitor: AppStateMonitor,
    var audioClient: AudioClient? = null
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
    override var currentAudioStatus: MeetingSessionStatusCode? = MeetingSessionStatusCode.OK

    private var audioClientStateObservers = ConcurrentSet.createConcurrentSet<AudioVideoObserver>()
    private var realtimeEventObservers = ConcurrentSet.createConcurrentSet<RealtimeObserver>()
    private var transcriptEventObservers = ConcurrentSet.createConcurrentSet<TranscriptEventObserver>()

    /**
     * Volume state change can be used to figure out the meeting's current attendees.
     * Keep track of attendees here to determine additions / removals to notify observers
     */
    private var currentAttendees = mutableSetOf<AttendeeInfo>()
    private var currentAttendeeVolumeMap = mapOf<String, VolumeUpdate>()
    private var currentAttendeeSignalMap = mapOf<String, SignalUpdate>()

    override var primaryMeetingPromotionObserver: PrimaryMeetingPromotionObserver? = null

    override fun onAudioClientStateChange(newInternalAudioState: Int, newInternalAudioStatus: Int) {
        val newAudioStatus: MeetingSessionStatusCode? = toAudioStatus(newInternalAudioStatus)
        val newAudioState: SessionStateControllerAction = toAudioClientState(newInternalAudioState, newAudioStatus)

        if (newAudioStatus == null) {
            logger.warn(
                TAG,
                "AudioClient State raw value: $newInternalAudioState Unknown Status raw value: $newInternalAudioStatus"
            )
        } else {
            logger.info(TAG, "AudioClient State: $newAudioState Status: $newAudioStatus")
        }

        if (newAudioState == SessionStateControllerAction.Unknown) return
        if (newAudioState == currentAudioState && newAudioStatus == currentAudioStatus) return

        when (newAudioState) {
            SessionStateControllerAction.FinishConnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting -> {
                        meetingStatsCollector.updateMeetingStartTimeMs()
                        eventAnalyticsController.publishEvent(
                            EventName.meetingStartSucceeded
                        )
                        notifyAudioClientObserver { observer -> observer.onAudioSessionStarted(false) }
                    }
                    SessionStateControllerAction.Reconnecting -> {
                        meetingStatsCollector.incrementRetryCount()
                        meetingStatsCollector.updateMeetingReconnectedTimeMs()
                        eventAnalyticsController.publishEvent(
                            EventName.meetingReconnected
                        )
                        notifyAudioClientObserver {
                            observer -> observer.onAudioSessionStarted(true)
                            primaryMeetingPromotionObserver?.onPrimaryMeetingDemotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioInternalServerError))
                                ?: run {
                                    logger.info(TAG, "Primary meeting demotion occurred from audio (on reconnect) but no primary meeting demotion callback is set")
                                }
                            primaryMeetingPromotionObserver = null
                        }
                    }
                    SessionStateControllerAction.FinishConnecting ->
                        when (newAudioStatus) {
                            MeetingSessionStatusCode.OK ->
                                if (currentAudioStatus == MeetingSessionStatusCode.NetworkBecamePoor) {
                                    notifyAudioClientObserver { observer -> observer.onConnectionRecovered() }
                                }
                            MeetingSessionStatusCode.NetworkBecamePoor ->
                                if (currentAudioStatus == MeetingSessionStatusCode.OK) {
                                    meetingStatsCollector.incrementPoorConnectionCount()
                                    notifyAudioClientObserver { observer -> observer.onConnectionBecamePoor() }
                                }
                            else -> Unit
                        }
                    else -> Unit
                }
            }
            SessionStateControllerAction.Reconnecting -> {
                meetingStatsCollector.updateMeetingStartReconnectingTimeMs()
                if (currentAudioState == SessionStateControllerAction.FinishConnecting) {
                    notifyAudioClientObserver { observer -> observer.onAudioSessionDropped() }
                }
            }
            SessionStateControllerAction.FinishDisconnecting -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting -> {
                        if (shouldCloseAndNotifyEndMeeting(newAudioStatus)) {
                            handleAudioSessionEndedByServer(newAudioStatus)
                        }
                    }
                    SessionStateControllerAction.Reconnecting -> {
                        notifyAudioClientObserver { observer -> observer.onAudioSessionCancelledReconnect() }
                        if (shouldCloseAndNotifyEndMeeting(newAudioStatus)) {
                            handleAudioSessionEndedByServer(newAudioStatus)
                        }
                    }
                    else -> Unit
                }
            }
            SessionStateControllerAction.Fail -> {
                when (currentAudioState) {
                    SessionStateControllerAction.Connecting,
                    SessionStateControllerAction.FinishConnecting -> {
                        handleOnAudioSessionFailed(newAudioStatus)
                    }
                    SessionStateControllerAction.Reconnecting -> {
                        notifyAudioClientObserver { observer ->
                            observer.onAudioSessionCancelledReconnect()
                        }
                        handleOnAudioSessionFailed(newAudioStatus)
                    }
                    else -> Unit
                }
            }
            else -> Unit
        }
        currentAudioState = newAudioState
        currentAudioStatus = newAudioStatus
    }

    override fun onVolumeStateChange(attendeeUpdates: Array<out AttendeeUpdate>?) {
        if (attendeeUpdates == null) return

        val newAttendeeVolumeMap: Map<String, VolumeUpdate> =
            attendeeUpdates.mapNotNull { attendeeUpdate ->
                VolumeLevel.from(attendeeUpdate.data)?.let {
                    val attendeeInfo = createAttendeeInfo(attendeeUpdate)
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
            ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
                it.onVolumeChanged(
                    volumeUpdates
                )
            }
        }

        currentAttendeeVolumeMap = newAttendeeVolumeMap
    }

    override fun onSignalStrengthChange(attendeeUpdates: Array<out AttendeeUpdate>?) {
        if (attendeeUpdates == null) return

        val newAttendeeSignalMap: Map<String, SignalUpdate> =
            attendeeUpdates
                .mapNotNull { attendeeUpdate ->
                    SignalStrength.from(attendeeUpdate.data)?.let {
                        val attendeeInfo = createAttendeeInfo(attendeeUpdate)
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
            ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
                it.onSignalStrengthChanged(
                    signalUpdates
                )
            }
        }

        currentAttendeeSignalMap = newAttendeeSignalMap
    }

    override fun onTranscriptEventsReceived(events: Array<out TranscriptEventInternal>?) {
        if (events == null) return

        events.forEach { rawEvent ->
            val event: TranscriptEvent?
            when (rawEvent) {
                is TranscriptionStatusInternal -> {
                    event = TranscriptionStatus(
                        TranscriptionStatusType.from(rawEvent.type.value),
                        rawEvent.eventTimeMs,
                        rawEvent.transcriptionRegion,
                        rawEvent.transcriptionConfiguration,
                        rawEvent.message
                    )
                }
                is TranscriptInternal -> {
                    val results = mutableListOf<TranscriptResult>()
                    rawEvent.results.forEach { rawResult ->
                        val alternatives = mutableListOf<TranscriptAlternative>()
                        rawResult.alternatives.forEach { rawAlternative ->
                            val items = mutableListOf<TranscriptItem>()
                            rawAlternative.items.forEach { rawItem ->
                                val item = TranscriptItem(
                                    TranscriptItemType.from(rawItem.type.value),
                                    rawItem.startTimeMs,
                                    rawItem.endTimeMs,
                                    AttendeeInfo(
                                        rawItem.attendee.attendeeId,
                                        rawItem.attendee.externalUserId
                                    ),
                                    rawItem.content,
                                    rawItem.vocabularyFilterMatch,
                                    rawItem.confidence,
                                    rawItem.stable
                                )
                                items.add(item)
                            }
                            val entities = rawAlternative.entities?.let {
                                it.map { rawEntity ->
                                    TranscriptEntity(
                                        rawEntity.category,
                                        rawEntity.confidence,
                                        rawEntity.content,
                                        rawEntity.startTimeMs,
                                        rawEntity.endTimeMs,
                                        rawEntity.type
                                    )
                                }.toTypedArray() ?: run { null }
                            }
                            val alternative = TranscriptAlternative(
                                items.toTypedArray(),
                                entities,
                                rawAlternative.transcript
                            )
                            alternatives.add(alternative)
                        }
                        val languageIdentification = rawResult.languageIdentification?.let {
                            it.map { rawLanguageIdentification ->
                                TranscriptLanguageWithScore(
                                    rawLanguageIdentification.languageCode,
                                    rawLanguageIdentification.score
                                )
                            }.toTypedArray() ?: run { null }
                        }
                        val result = TranscriptResult(
                            rawResult.resultId,
                            rawResult.channelId,
                            rawResult.isPartial,
                            rawResult.startTimeMs,
                            rawResult.endTimeMs,
                            alternatives.toTypedArray(),
                            rawResult.languageCode,
                            languageIdentification
                        )
                        results.add(result)
                    }
                    event = Transcript(results.toTypedArray())
                } else -> {
                    logger.error(TAG, "Received transcript event in unknown format")
                    event = null
                }
            }

            event?.let { transcriptEvent ->
                ObserverUtils.notifyObserverOnMainThread(transcriptEventObservers) {
                    it.onTranscriptEventReceived(transcriptEvent)
                }
            }
        }
    }

    private fun onAttendeesMuteStateChange(volumesDelta: Map<String, VolumeUpdate>) {
        val mutedAttendeeMap: Map<String, VolumeUpdate> = volumesDelta.filter { (_, value) ->
            value.volumeLevel == VolumeLevel.Muted
        }

        if (mutedAttendeeMap.isNotEmpty()) {
            val mutedAttendeeInfo: Array<AttendeeInfo> =
                mutedAttendeeMap.map { (_, value) -> value.attendeeInfo }.toTypedArray()

            ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
                it.onAttendeesMuted(
                    mutedAttendeeInfo
                )
            }
        }

        val unMutedAttendeeMap = volumesDelta.filter { (key, _) ->
            currentAttendeeVolumeMap[key]?.volumeLevel == VolumeLevel.Muted
        }

        if (unMutedAttendeeMap.isNotEmpty()) {
            val unMutedAttendeeInfo: Array<AttendeeInfo> =
                unMutedAttendeeMap.map { (_, value) -> value.attendeeInfo }.toTypedArray()

            ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
                it.onAttendeesUnmuted(
                    unMutedAttendeeInfo
                )
            }
        }
    }

    override fun onLogMessage(logLevel: Int, message: String?) {
        if (message == null) return

        // Only print error and fatal as the Media team's request to avoid noise for application
        // that has log level set to INFO or higher. All other cases, print as verbose
        if (logLevel == AudioClient.L_ERROR || logLevel == AudioClient.L_FATAL) {
            logger.error(TAG, message)
        } else {
            logger.verbose(TAG, message)
        }
    }

    override fun onMetrics(metrics: IntArray?, values: DoubleArray?) {
        if (metrics == null || values == null) return

        val metricMap = mutableMapOf<Int, Double>()
        (metrics.indices).map { i -> metricMap[metrics[i]] = values[i] }
        clientMetricsCollector.processAudioClientMetrics(metricMap)
    }

    /**
     * This will be called by media library whenever there is attendee presence change
     * There could be duplicate attendee join, but no duplicate left/drop will be provided
     * from media library side.
     */
    override fun onAttendeesPresenceChange(attendeeUpdates: Array<out AttendeeUpdate>?) {
        if (attendeeUpdates == null) return

        // group by attendee status
        val attendeesByStatus = attendeeUpdates
            .groupBy({ AttendeeStatus.from(it.data) }, { createAttendeeInfo(it) })

        attendeesByStatus[AttendeeStatus.Joined]?.let {
            val attendeeJoined = it.minus(currentAttendees)
            if (attendeeJoined.isNotEmpty()) {
                onAttendeesJoin(attendeeJoined.toTypedArray())
                currentAttendees.addAll(attendeeJoined)
            }
        }

        attendeesByStatus[AttendeeStatus.Left]?.let {
            onAttendeesLeft(it.toTypedArray())
            currentAttendees.removeAll(it)
        }

        attendeesByStatus[AttendeeStatus.Dropped]?.let {
            onAttendeesDropped(it.toTypedArray())
            currentAttendees.removeAll(it)
        }
    }

    // Create AttendeeInfo based on AttendeeUpdate, fill up external ID for local attendee
    private fun createAttendeeInfo(attendeeUpdate: AttendeeUpdate): AttendeeInfo {
        val externalUserId =
            if (attendeeUpdate.externalUserId.isEmpty() &&
                (attendeeUpdate.profileId == configuration.credentials.attendeeId)
            ) {
                configuration.credentials.externalUserId
            } else {
                attendeeUpdate.externalUserId
            }
        return AttendeeInfo(
            attendeeUpdate.profileId,
            externalUserId
        )
    }

    private fun onAttendeesJoin(attendeeInfo: Array<AttendeeInfo>) {
        logger.debug(TAG, "Joined: ${attendeeInfo.joinToString(" ")}")
        ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
            it.onAttendeesJoined(
                attendeeInfo
            )
        }
    }

    private fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        logger.debug(TAG, "Left: ${attendeeInfo.joinToString(" ")}")
        ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
            it.onAttendeesLeft(
                attendeeInfo
            )
        }
    }

    private fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        logger.debug(TAG, "Dropped: ${attendeeInfo.joinToString(" ")}")
        ObserverUtils.notifyObserverOnMainThread(realtimeEventObservers) {
            it.onAttendeesDropped(
                attendeeInfo
            )
        }
    }

    override fun onAudioClientPrimaryMeetingEvent(type: Int, status: Int) {
        logger.info(TAG, "Primary meeting event for notified with type $type and status $status")
        val sdkStatus = when (status) {
            AudioClient.AUDIO_CLIENT_PRIMARY_MEETING_OK -> MeetingSessionStatusCode.OK
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_DISCONNECTED -> MeetingSessionStatusCode.AudioDisconnected
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_INTERNAL_ERROR -> MeetingSessionStatusCode.AudioInternalServerError
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_AUTHENTICATION_FAILED -> MeetingSessionStatusCode.AudioAuthenticationRejected
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_REMOVED_FROM_MEETING -> MeetingSessionStatusCode.AudioDisconnectAudio
            else -> MeetingSessionStatusCode.AudioInternalServerError
        }

        when (type) {
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_JOIN_ACK -> {
                primaryMeetingPromotionObserver?.onPrimaryMeetingPromotion(MeetingSessionStatus(sdkStatus))
                    ?: run {
                    logger.info(TAG, "Primary meeting promotion completed from audio but no primary meeting promotion callback is set")
                }
            }
            AudioClient.AUDIO_SERVER_PRIMARY_MEETING_LEAVE -> {
                primaryMeetingPromotionObserver?.onPrimaryMeetingDemotion(MeetingSessionStatus(sdkStatus))
                    ?: run {
                        logger.info(TAG, "Primary meeting demotion occurred from audio but no primary meeting demotion callback is set")
                }
                primaryMeetingPromotionObserver = null
            }
        }
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

    override fun subscribeToTranscriptEvent(observer: TranscriptEventObserver) {
        transcriptEventObservers.add(observer)
    }

    override fun unsubscribeFromTranscriptEvent(observer: TranscriptEventObserver) {
        transcriptEventObservers.remove(observer)
    }

    override fun notifyAudioClientObserver(observerFunction: (observer: AudioVideoObserver) -> Unit) {
        ObserverUtils.notifyObserverOnMainThread(audioClientStateObservers, observerFunction)
    }

    private fun toAudioClientState(internalAudioClientState: Int, statusCode: MeetingSessionStatusCode?): SessionStateControllerAction {
        if (shouldCloseAndNotifyEndMeeting(statusCode)) {
            return SessionStateControllerAction.FinishDisconnecting
        }
        return when (internalAudioClientState) {
            AudioClient.AUDIO_CLIENT_STATE_UNKNOWN -> SessionStateControllerAction.Unknown
            AudioClient.AUDIO_CLIENT_STATE_INIT -> SessionStateControllerAction.Init
            AudioClient.AUDIO_CLIENT_STATE_CONNECTING -> SessionStateControllerAction.Connecting
            AudioClient.AUDIO_CLIENT_STATE_CONNECTED -> SessionStateControllerAction.FinishConnecting
            AudioClient.AUDIO_CLIENT_STATE_RECONNECTING -> SessionStateControllerAction.Reconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING -> SessionStateControllerAction.Disconnecting
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL -> SessionStateControllerAction.FinishDisconnecting
            AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
            AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
            AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL -> SessionStateControllerAction.Fail
            else -> SessionStateControllerAction.Unknown
        }
    }

    private fun shouldCloseAndNotifyEndMeeting(status: MeetingSessionStatusCode?): Boolean {
        return status == MeetingSessionStatusCode.AudioServerHungup || status == MeetingSessionStatusCode.AudioJoinedFromAnotherDevice
    }

    private fun toAudioStatus(internalAudioStatus: Int): MeetingSessionStatusCode? {
        return when (internalAudioStatus) {
            AudioClient.AUDIO_CLIENT_OK -> MeetingSessionStatusCode.OK
            AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP -> MeetingSessionStatusCode.NetworkBecamePoor
            AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP -> MeetingSessionStatusCode.AudioServerHungup
            AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE -> MeetingSessionStatusCode.AudioJoinedFromAnotherDevice
            AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR -> MeetingSessionStatusCode.AudioInternalServerError
            AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED -> MeetingSessionStatusCode.AudioAuthenticationRejected
            AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY -> MeetingSessionStatusCode.AudioCallAtCapacity
            AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE -> MeetingSessionStatusCode.AudioServiceUnavailable
            AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO -> MeetingSessionStatusCode.AudioDisconnectAudio
            AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED -> MeetingSessionStatusCode.AudioCallEnded
            AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING -> MeetingSessionStatusCode.AudioInputDeviceNotResponding
            AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING -> MeetingSessionStatusCode.AudioOutputDeviceNotResponding
            else -> null
        }
    }

    private fun handleOnAudioSessionFailed(statusCode: MeetingSessionStatusCode?) {
        if (audioClient != null) {
            notifyFailed(statusCode)
        }
        handleAudioClientStop(statusCode)
    }

    private fun handleAudioClientStop(statusCode: MeetingSessionStatusCode?) {
        if (audioClient != null) {
            // TODO: assess if only notifyAudioClientObserver should be in GlobalScope.launch
            GlobalScope.launch {
                audioClient?.stopSession()
                appStateMonitor.stop()
                DefaultAudioClientController.audioClientState = AudioClientState.STOPPED
                notifyAudioClientObserver { observer ->
                    observer.onAudioSessionStopped(MeetingSessionStatus(statusCode))
                }
            }
        } else {
            logger.error(TAG, "Failed to stop audio session since audioClient is null")
        }
    }

    private fun notifyFailed(statusCode: MeetingSessionStatusCode?) {
        val attributes: MutableMap<EventAttributeName, Any>? = statusCode?.let {
            mutableMapOf(
                EventAttributeName.meetingStatus to statusCode,
                EventAttributeName.meetingErrorMessage to statusCode.toString()
            )
        }
        eventAnalyticsController.publishEvent(EventName.meetingFailed, attributes)
        meetingStatsCollector.resetMeetingStats()
    }

    private fun notifyMeetingEnded(statusCode: MeetingSessionStatusCode?) {
        val attributes: MutableMap<EventAttributeName, Any>? = statusCode?.let {
            mutableMapOf(
                EventAttributeName.meetingStatus to statusCode,
                EventAttributeName.meetingErrorMessage to statusCode.toString()
            )
        }
        eventAnalyticsController.publishEvent(EventName.meetingEnded, attributes)
        meetingStatsCollector.resetMeetingStats()
    }

    private fun handleAudioSessionEndedByServer(statusCode: MeetingSessionStatusCode?) {
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            return
        }
        notifyMeetingEnded(statusCode)
        handleAudioClientStop(statusCode)
    }
}
