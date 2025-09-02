/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.TestConstant
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.Transcript
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptAlternative
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptEntity
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
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.TranscriptEventObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AttendeeInfo as AttendeeInfoInternal
import com.xodee.client.audio.audioclient.AttendeeUpdate
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.transcript.Transcript as TranscriptInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptAlternative as TranscriptAlternativeInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptEntity as TranscriptEntityInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptEvent as TranscriptEventInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptItem as TranscriptItemInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptItemType as TranscriptItemTypeInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptLanguageWithScore as TranscriptLanguageWithScoreInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptResult as TranscriptResultInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptionStatus as TranscriptionStatusInternal
import com.xodee.client.audio.audioclient.transcript.TranscriptionStatusType as TranscriptionStatusTypeInternal
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultAudioClientObserverTest {
    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockClientMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var mockConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var mockAppStateMonitor: AppStateMonitor

    @MockK
    private lateinit var mockRealtimeObserver: RealtimeObserver

    @MockK
    private lateinit var mockTranscriptEventObserver: TranscriptEventObserver

    @MockK
    private lateinit var mockAudioClient: AudioClient

    @MockK
    private lateinit var mockMeetingStatsCollector: MeetingStatsCollector

    @MockK
    private lateinit var mockEventAnalyticsController: EventAnalyticsController

    private lateinit var audioClientObserver: DefaultAudioClientObserver

    private val testObserverFun = { observer: AudioVideoObserver ->
        observer.onAudioSessionStartedConnecting(
            false
        )
    }

    private val testId1 = "aliceId"
    private val testId2 = "bobId"
    private val testIdLocal = "me myself & I"
    private val invalidInput = 1000

    private val testAttendeeVolumeUpdate =
        AttendeeUpdate(testId1, testId1, VolumeLevel.High.value)
    private val testAttendeeSignalUpdate =
        AttendeeUpdate(testId1, testId1, SignalStrength.High.value)
    private val testAttendeeVolumeMuted =
        AttendeeUpdate(testId1, testId1, VolumeLevel.Muted.value)
    private val testAttendeeUpdateJoined =
        AttendeeUpdate(testId1, testId1, AttendeeStatus.Joined.value)
    private val testAttendeeUpdateLeft =
        AttendeeUpdate(testId1, testId1, AttendeeStatus.Left.value)
    private val testAttendeeUpdateDropped =
        AttendeeUpdate(testId1, testId1, AttendeeStatus.Dropped.value)
    private val testAttendeeInfo =
        AttendeeInfo(testId1, testId1)
    private val testVolumeUpdate =
        VolumeUpdate(
            testAttendeeInfo,
            VolumeLevel.High
        )
    private val testSignalUpdate =
        SignalUpdate(
            testAttendeeInfo,
            SignalStrength.High
        )
    private val testDispatcher = TestCoroutineDispatcher()

    private val expectedAttendeeInfos: Array<AttendeeInfo> = arrayOf(testAttendeeInfo)

    private val timestampMs = 1633040215L
    private val transcriptionRegion = "us-east-1"
    private val transcriptionConfiguration = "transcription-configuration"

    @Before
    fun setup() {
        // Mock Log.d first because initializing the AudioClient mock appears to fail otherwise
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioClientObserver =
            DefaultAudioClientObserver(
                mockLogger,
                mockClientMetricsCollector,
                mockConfiguration,
                mockMeetingStatsCollector,
                mockEventAnalyticsController,
                mockAppStateMonitor
            )
        audioClientObserver.subscribeToAudioClientStateChange(mockAudioVideoObserver)
        audioClientObserver.subscribeToRealTimeEvents(mockRealtimeObserver)
        audioClientObserver.audioClient = mockAudioClient
        every { mockConfiguration.credentials.attendeeId } returns testIdLocal
        every { mockConfiguration.credentials.externalUserId } returns testIdLocal
        every { mockAudioClient.stopSession() } returns 0
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    // notifyAudioClientObserver tests

    @Test
    fun `notifyAudioClientObserver should notify added observers`() {
        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verify(exactly = 1) { mockAudioVideoObserver.onAudioSessionStartedConnecting(any()) }
    }

    // unsubscribeFromAudioClientStateChange tests

    @Test
    fun `unsubscribeFromAudioClientStateChange should result in no notification`() {
        audioClientObserver.unsubscribeFromAudioClientStateChange(mockAudioVideoObserver)

        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verifyAudioVideoObserverIsNotNotified()
    }

    // onAttendeesPresenceChange tests

    @Test
    fun `onAttendeesPresenceChange should NOT notify about attendee presence events when NO attendees updates`() {
        audioClientObserver.onAttendeesPresenceChange(emptyArray())

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesJoined(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesLeft(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesDropped(any()) }
    }

    @Test
    fun `onAttendeesPresenceChange should notify added observers when a new attendee joined`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoined(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should notify added observers when an attendee left`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateLeft
        ))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesLeft(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should notify added observers when an attendee got dropped`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateDropped
        ))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesDropped(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should NOT notify added observers twice when duplicate attendee joined`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoined(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should notify added observers when attendee who had left rejoined`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateLeft
        ))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))

        verify(exactly = 2) { mockRealtimeObserver.onAttendeesJoined(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should notify added observers with rejoined attendee who has dropped`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateDropped
        ))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(
            testAttendeeUpdateJoined
        ))

        verify(exactly = 2) { mockRealtimeObserver.onAttendeesJoined(expectedAttendeeInfos) }
    }

    @Test
    fun `onAttendeesPresenceChange should NOT consider attendee as same only when externalUserId is same`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(testAttendeeUpdateJoined))
        audioClientObserver.onAttendeesPresenceChange(
            arrayOf(
                AttendeeUpdate(
                    testId2,
                    testAttendeeUpdateJoined.externalUserId,
                    testAttendeeUpdateJoined.data
                )
            )
        )

        verifyOrder {
            mockRealtimeObserver.onAttendeesJoined(arrayOf(testAttendeeInfo))
            mockRealtimeObserver.onAttendeesJoined(
                arrayOf(
                    AttendeeInfo(
                        testId2,
                        testAttendeeInfo.externalUserId
                    )
                )
            )
        }
    }

    @Test
    fun `onAttendeesPresenceChange should consider attendee as same when attendeeId and externalUserId are same`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(testAttendeeUpdateJoined))
        audioClientObserver.onAttendeesPresenceChange(
            arrayOf(
                AttendeeUpdate(
                    testAttendeeUpdateJoined.profileId,
                    testAttendeeUpdateJoined.externalUserId,
                    testAttendeeUpdateJoined.data
                )
            )
        )

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoined(arrayOf(testAttendeeInfo)) }
    }

    @Test
    fun `onAttendeesPresenceChange should consider attendee as different when externalUserId is different`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(testAttendeeUpdateJoined))
        audioClientObserver.onAttendeesPresenceChange(
            arrayOf(
                AttendeeUpdate(
                    testAttendeeUpdateJoined.profileId,
                    testId2,
                    testAttendeeUpdateJoined.data
                )
            )
        )

        verifyOrder {
            mockRealtimeObserver.onAttendeesJoined(arrayOf(testAttendeeInfo))
            mockRealtimeObserver.onAttendeesJoined(
                arrayOf(
                    AttendeeInfo(
                        testAttendeeInfo.attendeeId,
                        testId2
                    )
                )
            )
        }
    }

    @Test
    fun `onAttendeesPresenceChange should ONLY notify delta on subsequent calls`() {
        audioClientObserver.onAttendeesPresenceChange(arrayOf(testAttendeeUpdateJoined))
        audioClientObserver.onAttendeesPresenceChange(
            arrayOf(
                testAttendeeUpdateJoined,
                AttendeeUpdate(
                    testId2,
                    testId2,
                    testAttendeeUpdateJoined.data
                )
            )
        )

        verifyOrder {
            mockRealtimeObserver.onAttendeesJoined(arrayOf(testAttendeeInfo))
            mockRealtimeObserver.onAttendeesJoined(
                arrayOf(
                    AttendeeInfo(
                        testId2,
                        testId2
                    )
                )
            )
        }
    }

    @Test
    fun `onAttendeesPresenceChange should send local attendee event with non-empty externalUserId`() {
        val testInput = arrayOf(
            AttendeeUpdate(testIdLocal, "", AttendeeStatus.Joined.value)
        )
        val expectedArgs: Array<AttendeeInfo> = arrayOf(
            AttendeeInfo(testIdLocal, testIdLocal)
        )

        audioClientObserver.onAttendeesPresenceChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoined(expectedArgs) }
    }

    // onVolumeStateChange tests

    @Test
    fun `onVolumeStateChange should notify added observers`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChanged(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no attendee updates`() {
        audioClientObserver.onVolumeStateChange(null)

        verify(exactly = 0) { mockRealtimeObserver.onVolumeChanged(any()) }
    }

    @Test
    fun `onVolumeStateChange should only notify delta`() {
        val testAttendeeUpdate2 =
            AttendeeUpdate(testId2, testId2, VolumeLevel.Low.value)
        val expectedArgs1 = arrayOf(testVolumeUpdate)
        val expectedArgs2 = arrayOf(
            VolumeUpdate(
                AttendeeInfo(testId2, testId2),
                VolumeLevel.Low
            )
        )

        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(
            arrayOf(
                testAttendeeVolumeUpdate,
                testAttendeeUpdate2
            )
        )

        verifyOrder {
            mockRealtimeObserver.onVolumeChanged(expectedArgs1)
            mockRealtimeObserver.onVolumeChanged(expectedArgs2)
        }
    }

    // onVolumeChange tests

    @Test
    fun `onVolumeChange should send remote attendee event when it has an empty externalUserId`() {
        val testInput = arrayOf(
            testAttendeeVolumeUpdate,
            AttendeeUpdate(testId2, "", VolumeLevel.Low.value)
        )
        val expectedArgs: Array<VolumeUpdate> = arrayOf(
            testVolumeUpdate,
            VolumeUpdate(AttendeeInfo(testId2, ""), VolumeLevel.Low)
        )

        audioClientObserver.onVolumeStateChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChanged(expectedArgs) }
    }

    @Test
    fun `onVolumeChange should filter out invalid volume levels`() {
        val testInput = arrayOf(
            testAttendeeVolumeUpdate,
            AttendeeUpdate(testId2, testId2, invalidInput)
        )
        val expectedArgs: Array<VolumeUpdate> = arrayOf(testVolumeUpdate)

        audioClientObserver.onVolumeStateChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChanged(expectedArgs) }
    }

    // onSignalStrengthChange tests

    @Test
    fun `onSignalStrengthChange should notify added observers`() {
        audioClientObserver.onSignalStrengthChange(arrayOf(testAttendeeSignalUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChanged(any()) }
    }

    @Test
    fun `onSignalStrengthChange should NOT notify when no attendee updates`() {
        audioClientObserver.onSignalStrengthChange(null)

        verify(exactly = 0) { mockRealtimeObserver.onSignalStrengthChanged(any()) }
    }

    @Test
    fun `onSignalStrengthChange should only notify delta`() {
        val testAttendeeUpdate2 =
            AttendeeUpdate(testId2, testId2, SignalStrength.Low.value)
        val expectedArgs1 = arrayOf(testSignalUpdate)
        val expectedArgs2 =
            arrayOf(
                SignalUpdate(
                    AttendeeInfo(
                        testId2,
                        testId2
                    ), SignalStrength.Low
                )
            )

        audioClientObserver.onSignalStrengthChange(arrayOf(testAttendeeSignalUpdate))
        audioClientObserver.onSignalStrengthChange(
            arrayOf(
                testAttendeeSignalUpdate,
                testAttendeeUpdate2
            )
        )

        verifyOrder {
            mockRealtimeObserver.onSignalStrengthChanged(expectedArgs1)
            mockRealtimeObserver.onSignalStrengthChanged(expectedArgs2)
        }
    }

    @Test
    fun `onSignalStrengthChange should send remote attendee event when it has an empty externalUserId`() {
        val testInput = arrayOf(
            testAttendeeSignalUpdate,
            AttendeeUpdate(testId2, "", SignalStrength.High.value)
        )
        val expectedArgs: Array<SignalUpdate> = arrayOf(
            testSignalUpdate,
            SignalUpdate(AttendeeInfo(testId2, ""), SignalStrength.High)
        )

        audioClientObserver.onSignalStrengthChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChanged(expectedArgs) }
    }

    @Test
    fun `onSignalStrengthChange should filter out invalid signal strengths`() {
        val testInput = arrayOf(
            testAttendeeSignalUpdate,
            AttendeeUpdate(testId2, testId2, invalidInput)
        )
        val expectedArgs: Array<SignalUpdate> = arrayOf(testSignalUpdate)

        audioClientObserver.onSignalStrengthChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChanged(expectedArgs) }
    }

    @Test
    fun `onSignalStrengthChange should send local attendee event with non-empty externalUserId`() {
        val testInput = arrayOf(
            AttendeeUpdate(testIdLocal, "", SignalStrength.High.value)
        )
        val expectedArgs: Array<SignalUpdate> = arrayOf(
            SignalUpdate(AttendeeInfo(testIdLocal, testIdLocal), SignalStrength.High)
        )

        audioClientObserver.onSignalStrengthChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChanged(expectedArgs) }
    }

    // onVolumeStateChange tests

    @Test
    fun `onVolumeStateChange should notify about attendee mute when newly muted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesMuted(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify about attendee mute when NO newly muted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesMuted(any()) }
    }

    @Test
    fun `onVolumeStateChange should notify about attendee unmute newly unmuted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesUnmuted(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify about attendee unmute when NO newly unmuted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesUnmuted(any()) }
    }

    @Test
    fun `onVolumeStateChange should send local attendee event with non-empty externalUserId`() {
        val testInput = arrayOf(
            AttendeeUpdate(testIdLocal, "", VolumeLevel.High.value)
        )
        val expectedArgs: Array<VolumeUpdate> = arrayOf(
            VolumeUpdate(AttendeeInfo(testIdLocal, testIdLocal), VolumeLevel.High)
        )

        audioClientObserver.onVolumeStateChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChanged(expectedArgs) }
    }

    // onTranscriptEventsReceived tests

    @Test
    fun `onTranscriptEventsReceived should do nothing if transcript events is sent as null`() {
        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        audioClientObserver.onTranscriptEventsReceived(null)

        verify(exactly = 0) { mockTranscriptEventObserver.onTranscriptEventReceived(any()) }
    }

    @Test
    fun `onTranscriptEventsReceived should send local Transcription Status with TranscriptionStatus events as input`() {
        val successStatusMessage = "success-message"
        val resumedStatusMessage = "resumed-message"

        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        val transcriptionStatusStarted = TranscriptionStatusInternal(
            TranscriptionStatusTypeInternal.TranscriptionStatusTypeStarted,
            timestampMs,
            transcriptionRegion,
            transcriptionConfiguration,
            successStatusMessage
        )

        val transcriptionStatusResumed = TranscriptionStatusInternal(
            TranscriptionStatusTypeInternal.TranscriptionStatusTypeResumed,
            timestampMs,
            transcriptionRegion,
            transcriptionConfiguration,
            resumedStatusMessage
        )

        val expectedTranscriptionStatusStarted = TranscriptionStatus(
            TranscriptionStatusType.Started,
            timestampMs,
            transcriptionRegion,
            transcriptionConfiguration,
            successStatusMessage
        )

        val expectedTranscriptionStatusResume = TranscriptionStatus(
            TranscriptionStatusType.Resumed,
            timestampMs,
            transcriptionRegion,
            transcriptionConfiguration,
            resumedStatusMessage
        )

        val events: Array<TranscriptEventInternal> = arrayOf(transcriptionStatusStarted, transcriptionStatusResumed)

        audioClientObserver.onTranscriptEventsReceived(events)

        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscriptionStatusStarted) }
        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscriptionStatusResume) }
    }

    @Test
    fun `onTranscriptEventsReceived should send local Transcript with Transcript events as input`() {
        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        val testResultId = "testResultId"
        val testChannelId = "testChannelId"
        val isPartial = true

        val transcriptResultOne = TranscriptResultInternal(
            testResultId,
            testChannelId,
            isPartial,
            timestampMs,
            timestampMs + 10L,
            arrayOf(
                TranscriptAlternativeInternal(
                    arrayOf(
                        TranscriptItemInternal(
                            TranscriptItemTypeInternal.TranscriptItemTypePronunciation,
                            timestampMs,
                            timestampMs + 5L,
                            AttendeeInfoInternal(testId1, testId1),
                            "I",
                            true,
                            true,
                            0.0
                        ),
                        TranscriptItemInternal(
                            TranscriptItemTypeInternal.TranscriptItemTypePunctuation,
                            timestampMs + 5L,
                            timestampMs + 10L,
                            AttendeeInfoInternal(testId2, testId2),
                            "am",
                            false,
                            true,
                            0.0
                        )
                    ),
                    arrayOf(),
                    "I am"
                )
            ),
            null,
            null
        )

        val transcriptResultTwo = TranscriptResultInternal(
            testResultId,
            testChannelId,
            isPartial,
            timestampMs + 10L,
            timestampMs + 20L,
            arrayOf(
                TranscriptAlternativeInternal(
                    arrayOf(
                        TranscriptItemInternal(
                            TranscriptItemTypeInternal.TranscriptItemTypePunctuation,
                            timestampMs + 10,
                            timestampMs + 15L,
                            AttendeeInfoInternal(testId2, testId2),
                            "a",
                            true,
                            true,
                            0.0
                        ),
                        TranscriptItemInternal(
                            TranscriptItemTypeInternal.TranscriptItemTypePronunciation,
                            timestampMs + 15L,
                            timestampMs + 20L,
                            AttendeeInfoInternal(testId1, testId1),
                            "guardian",
                            false,
                            true,
                            0.0
                        )
                    ),
                    arrayOf(),
                    "a guardian"
                )
            ),
            null,
            null
        )

        val events: Array<TranscriptEventInternal> = arrayOf(
            TranscriptInternal(arrayOf(transcriptResultOne)),
            TranscriptInternal(arrayOf(transcriptResultTwo))
        )

        val expectedTranscriptOne = Transcript(
            arrayOf(
                TranscriptResult(
                    testResultId,
                    testChannelId,
                    isPartial,
                    timestampMs,
                    timestampMs + 10L,
                    arrayOf(
                        TranscriptAlternative(
                            arrayOf(
                                TranscriptItem(
                                    TranscriptItemType.Pronunciation,
                                    timestampMs,
                                    timestampMs + 5L,
                                    AttendeeInfo(testId1, testId1),
                                    "I",
                                    true,
                                    0.0,
                                    true
                                ),
                                TranscriptItem(
                                    TranscriptItemType.Punctuation,
                                    timestampMs + 5L,
                                    timestampMs + 10L,
                                    AttendeeInfo(testId2, testId2),
                                    "am",
                                    false,
                                    0.0,
                                    true
                                )
                            ),
                            arrayOf(),
                            "I am"
                        )
                    ),
                    null,
                    null
                )
            )
        )

        val expectedTranscriptTwo = Transcript(
            arrayOf(
                TranscriptResult(
                    testResultId,
                    testChannelId,
                    isPartial,
                    timestampMs + 10L,
                    timestampMs + 20L,
                    arrayOf(
                        TranscriptAlternative(
                            arrayOf(
                                TranscriptItem(
                                    TranscriptItemType.Punctuation,
                                    timestampMs + 10L,
                                    timestampMs + 15L,
                                    AttendeeInfo(testId2, testId2),
                                    "a",
                                    true,
                                    0.0,
                                    true
                                ),
                                TranscriptItem(
                                    TranscriptItemType.Pronunciation,
                                    timestampMs + 15L,
                                    timestampMs + 20L,
                                    AttendeeInfo(testId1, testId1),
                                    "guardian",
                                    false,
                                    0.0,
                                    true
                                )
                            ),
                            arrayOf(),
                            "a guardian"
                        )
                    ),
                    null,
                    null
                )
            )
        )

        audioClientObserver.onTranscriptEventsReceived(events)
        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscriptOne) }
        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscriptTwo) }
    }

    @Test
    fun `onTranscriptEventsReceived should send local Transcript with Transcript Entity events as input`() {
        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        val testResultId = "testResultId"
        val testChannelId = "testChannelId"
        val isPartial = true

        val transcriptResultItem = TranscriptItemInternal(TranscriptItemTypeInternal.TranscriptItemTypePronunciation,
            timestampMs, timestampMs + 5L, AttendeeInfoInternal(testId1, testId1), "I", true, true, 0.0)

        val transcriptResultEntity = TranscriptEntityInternal("PII", "NAME", "John Doe", 1.0, timestampMs + 5L, timestampMs + 10L)

        val transcriptResultAlternative = TranscriptAlternativeInternal(arrayOf(transcriptResultItem), arrayOf(transcriptResultEntity), "I am")

        val transcriptResult = TranscriptResultInternal(testResultId, testChannelId, isPartial, timestampMs, timestampMs + 10L, arrayOf(transcriptResultAlternative), null, arrayOf())

        val events: Array<TranscriptEventInternal> = arrayOf(TranscriptInternal(arrayOf(transcriptResult)))

        val expectedTranscriptItem = TranscriptItem(TranscriptItemType.Pronunciation, timestampMs, timestampMs + 5L,
            AttendeeInfo(testId1, testId1), "I", true, 0.0, true)

        val expectedTranscriptEntity = TranscriptEntity("NAME", 1.0, "John Doe", timestampMs + 5L, timestampMs + 10L, "PII")

        val expectedTranscriptAlternative = TranscriptAlternative(arrayOf(expectedTranscriptItem), arrayOf(expectedTranscriptEntity),
            "I am")

        val expectedTranscript = Transcript(arrayOf(TranscriptResult(testResultId, testChannelId, isPartial,
            timestampMs, timestampMs + 10L, arrayOf(expectedTranscriptAlternative), null, arrayOf())))

        audioClientObserver.onTranscriptEventsReceived(events)
        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscript) }
    }

    @Test
    fun `onTranscriptEventsReceived should send local Transcript with Transcript LanguageWithScore events as input`() {
        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        val testResultId = "testResultId"
        val testChannelId = "testChannelId"
        val isPartial = true
        val languageCode = "en-US"

        val transcriptResultItem = TranscriptItemInternal(TranscriptItemTypeInternal.TranscriptItemTypePronunciation,
            timestampMs, timestampMs + 5L, AttendeeInfoInternal(testId1, testId1), "I", true, true, 0.0)

        val transcriptLanguageWithScoreOne = TranscriptLanguageWithScoreInternal("en-US", 0.78)

        val transcriptLanguageWithScoreTwo = TranscriptLanguageWithScoreInternal("ja-JP", 0.22)

        val transcriptResultAlternative = TranscriptAlternativeInternal(arrayOf(transcriptResultItem), null, "I am")

        val transcriptResult = TranscriptResultInternal(testResultId, testChannelId, isPartial, timestampMs, timestampMs + 10L, arrayOf(transcriptResultAlternative), languageCode, arrayOf(transcriptLanguageWithScoreOne, transcriptLanguageWithScoreTwo))

        val events: Array<TranscriptEventInternal> = arrayOf(TranscriptInternal(arrayOf(transcriptResult)))

        val expectedTranscriptItem = TranscriptItem(TranscriptItemType.Pronunciation, timestampMs, timestampMs + 5L,
            AttendeeInfo(testId1, testId1), "I", true, 0.0, true)

        val expectedTranscriptLanguageWithScoreOne = TranscriptLanguageWithScore("en-US", 0.78)

        val expectedTranscriptLanguageWithScoreTwo = TranscriptLanguageWithScore("ja-JP", 0.22)

        val expectedTranscriptAlternative = TranscriptAlternative(arrayOf(expectedTranscriptItem), null, "I am")

        val expectedTranscript = Transcript(arrayOf(TranscriptResult(testResultId, testChannelId, isPartial,
            timestampMs, timestampMs + 10L, arrayOf(expectedTranscriptAlternative), languageCode, arrayOf(expectedTranscriptLanguageWithScoreOne, expectedTranscriptLanguageWithScoreTwo))))

        audioClientObserver.onTranscriptEventsReceived(events)
        verify(exactly = 1) { mockTranscriptEventObserver.onTranscriptEventReceived(expectedTranscript) }
    }

    @Test
    fun `onTranscriptEventsReceived should return if null transcript events is sent`() {
        audioClientObserver.subscribeToTranscriptEvent(mockTranscriptEventObserver)

        audioClientObserver.onTranscriptEventsReceived(null)

        verify(exactly = 0) { mockTranscriptEventObserver.onTranscriptEventReceived(any()) }
    }

    // unsubscribeFromRealTimeEvents tests

    @Test
    fun `unsubscribeFromRealTimeEvents should result in no notification`() {
        audioClientObserver.unsubscribeFromRealTimeEvents(mockRealtimeObserver)

        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onSignalStrengthChange(arrayOf(testAttendeeSignalUpdate))
        audioClientObserver.onAttendeesPresenceChange(arrayOf(testAttendeeUpdateJoined))

        verify(exactly = 0) { mockRealtimeObserver.onVolumeChanged(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesJoined(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesLeft(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesMuted(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesUnmuted(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onSignalStrengthChanged(any()) }
    }

    // Common sense tests for onAudioClientStateChange

    @Test
    fun `onAudioClientStateChange should NOT notify when new state is UNKNOWN`() {
        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                100,
                100
            )
        }

        verifyAudioVideoObserverIsNotNotified()
    }

    @Test
    fun `onAudioClientStateChange should notify of session started event when finished connecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) { mockAudioVideoObserver.onAudioSessionStarted(false) }
    }

    @Test
    fun `onAudioClientStateChange should publish meeting start event when finished connecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) { mockMeetingStatsCollector.updateMeetingStartTimeMs() }
        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(EventName.meetingStartSucceeded) }
    }

    @Test
    fun `onAudioClientStateChange should should publish meeting start event when finished reconnecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) { mockMeetingStatsCollector.incrementRetryCount() }
        verify(exactly = 1) { mockMeetingStatsCollector.updateMeetingReconnectedTimeMs() }
        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(EventName.meetingReconnected) }
    }

    @Test
    fun `onAudioClientStateChange should notify of session reconnected event when finished reconnecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) { mockAudioVideoObserver.onAudioSessionStarted(true) }
    }

    @Test
    fun `onAudioClientStateChange should notify of session reconnect event when start reconnecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) {
            mockMeetingStatsCollector.updateMeetingStartReconnectingTimeMs()
            mockAudioVideoObserver.onAudioSessionDropped()
        }
    }

    @Test
    fun `onAudioClientStateChange should increment poor connection count`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }

        verify(exactly = 1) { mockMeetingStatsCollector.incrementPoorConnectionCount() }
    }

    @Test
    fun `onAudioClientStateChange should notify of poor connection event when status changed to poor network`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }

        verify(exactly = 1) { mockAudioVideoObserver.onConnectionBecamePoor() }
    }

    @Test
    fun `onAudioClientStateChange should notify of connection recover event when status changed from poor network`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verify(exactly = 1) { mockAudioVideoObserver.onConnectionRecovered() }
    }

    @Test
    fun `onAudioClientStateChange should notify of session reconnect cancel event when cancelling reconnect`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }

        verify(exactly = 1) { mockAudioVideoObserver.onAudioSessionCancelledReconnect() }
    }

    @Test
    fun `onAudioClientStateChange should not notify of session stop event when disconnected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }

        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
    }

    @Test
    fun `onAudioClientStateChange should stop session and notify of session stop event when failure while connecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    @Test
    fun `onAudioClientStateChange should stop session and notify of session stop event when finish disconnecting while connecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingEnded, any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAppStateMonitor.stop() }
    }

    fun `onAudioClientStateChange should stop session and notify of session stop event when finish disconnecting while connected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingEnded, any()) }
    }

    fun `onAudioClientStateChange should stop session and notify of session stop event when finish disconnecting while reconnecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingEnded, any()) }
    }

    @Test
    fun `onAudioClientStateChange should notify of session fail event to EventAnalyticsController when failed to connect`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) {
            mockEventAnalyticsController.publishEvent(EventName.meetingFailed, any())
        }
    }

    @Test
    fun `onAudioClientStateChange should stop session and notify of session stop event when failure while reconnecting`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        coVerify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    @Test
    fun `onAudioClientStateChange should stop session and notify of session stop event when input device is not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        coVerify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(
            MeetingSessionStatus(MeetingSessionStatusCode.AudioInputDeviceNotResponding)
        ) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    @Test
    fun `onAudioClientStateChange should stop session and notify of session stop event when output device is not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_RECONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        coVerify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(
            MeetingSessionStatus(MeetingSessionStatusCode.AudioOutputDeviceNotResponding)
        ) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    @Test
    fun `onAudioClientStateChange should NOT notify when state and status is unchanged`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                SessionStateControllerAction.Init.value,
                MeetingSessionStatusCode.OK.value
            )
        }

        verifyAudioVideoObserverIsNotNotified()
    }

    // onAudioClientStateChange tests for all remaining combinations of status and state
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verifyAudioVideoObserverIsNotNotified()
        verify(exactly = 0) { mockEventAnalyticsController.publishEvent(any()) }
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_OK
            )
        }
        verifyFailed(MeetingSessionStatusCode.OK)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_OK
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_OK
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_OK
            )
        }
        verifyFailed(MeetingSessionStatusCode.OK)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup ok`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_OK
            )
        }
        verifyFailed(MeetingSessionStatusCode.OK)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }
        verifyFailed(MeetingSessionStatusCode.NetworkBecamePoor)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }
        verifyFailed(MeetingSessionStatusCode.NetworkBecamePoor)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup network not good enough for voip`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_STATUS_NETWORK_IS_NOT_GOOD_ENOUGH_FOR_VOIP
            )
        }
        verifyFailed(MeetingSessionStatusCode.NetworkBecamePoor)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup server hungup`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SERVER_HUNGUP
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioServerHungup)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }

        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup joined from another device`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_JOINED_FROM_ANOTHER_DEVICE
            )
        }
        verifyEnded(MeetingSessionStatusCode.AudioJoinedFromAnotherDevice)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInternalServerError)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInternalServerError)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup internal server error`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_INTERNAL_SERVER_ERROR
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInternalServerError)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioAuthenticationRejected)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioAuthenticationRejected)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup auth rejected`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_AUTH_REJECTED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioAuthenticationRejected)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }

        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallAtCapacity)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallAtCapacity)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup call at capacity`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_CALL_AT_CAPACITY
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallAtCapacity)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioServiceUnavailable)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioServiceUnavailable)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup service unavailable`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SERVICE_UNAVAILABLE
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioServiceUnavailable)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioDisconnectAudio)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioDisconnectAudio)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup should disconnect audio`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_SHOULD_DISCONNECT_AUDIO
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioDisconnectAudio)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallEnded)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallEnded)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup call ended`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_CALL_ENDED
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioCallEnded)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInputDeviceNotResponding)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInputDeviceNotResponding)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup audio input device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_INPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioInputDeviceNotResponding)
    }

    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to unknown audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_UNKNOWN,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to init audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_INIT,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to connecting audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to failed audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_FAILED_TO_CONNECT,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioOutputDeviceNotResponding)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnecting audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTING,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected normal audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_NORMAL,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyNoop()
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to disconnected abnormal audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_DISCONNECTED_ABNORMAL,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioOutputDeviceNotResponding)
    }
    @Test
    fun `onAudioClientStateChange should behave as expected when transitioning from connected ok to server hungup audio output device not responding`() {

        runBlockingTest {
            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_CONNECTED,
                AudioClient.AUDIO_CLIENT_OK
            )

            audioClientObserver.onAudioClientStateChange(
                AudioClient.AUDIO_CLIENT_STATE_SERVER_HUNGUP,
                AudioClient.AUDIO_CLIENT_ERR_OUTPUT_DEVICE_NOT_RESPONDING
            )
        }
        verifyFailed(MeetingSessionStatusCode.AudioOutputDeviceNotResponding)
    }

    // Logger tests

    @Test
    fun `onLogMessage should call logger`() {
        audioClientObserver.onLogMessage(AudioClient.L_ERROR, testId1)

        verify(exactly = 1) { mockLogger.error(any(), any()) }
    }

    @Test
    fun `onLogMessage should NOT call logger when message is null`() {
        audioClientObserver.onLogMessage(AudioClient.L_ERROR, null)

        verify(exactly = 0) { mockLogger.error(any(), any()) }
    }

    @Test
    fun `onLogMessage should NOT call logger when log level is non error or non fatal`() {
        audioClientObserver.onLogMessage(AudioClient.L_INFO, testId1)

        verify(exactly = 0) { mockLogger.error(any(), any()) }
    }

    // onMetrics tests

    @Test
    fun `onMetrics should call clientMetricsCollector processAudioClientMetrics`() {
        val metrics = mutableMapOf(1 to 2.3, 4 to 5.6)
        audioClientObserver.onMetrics(metrics.keys.toIntArray(), metrics.values.toDoubleArray())

        verify { mockClientMetricsCollector.processAudioClientMetrics(metrics) }
    }

    private fun verifyAudioVideoObserverIsNotNotified() {
        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionStartedConnecting(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionStarted(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionCancelledReconnect() }
        verify(exactly = 0) { mockAudioVideoObserver.onConnectionRecovered() }
        verify(exactly = 0) { mockAudioVideoObserver.onConnectionBecamePoor() }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoSessionStartedConnecting() }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoSessionStarted(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoSessionStopped(any()) }
    }

    private fun verifyFailed(statusCode: MeetingSessionStatusCode) {
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingFailed, any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(MeetingSessionStatus(statusCode)) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    private fun verifyEnded(statusCode: MeetingSessionStatusCode) {
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingEnded, any()) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioVideoObserver.onAudioSessionStopped(MeetingSessionStatus(statusCode)) }
        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
    }

    private fun verifyNoop() {
        verifyAudioVideoObserverIsNotNotified()
        verify(exactly = 0) { mockEventAnalyticsController.publishEvent(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioSessionStopped(any()) }
        verify(exactly = 0) { mockAudioClient.stopSession() }
    }
}
