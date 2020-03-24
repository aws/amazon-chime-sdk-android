/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AttendeeUpdate
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
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
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var mockRealtimeObserver: RealtimeObserver

    private lateinit var audioClientObserver: DefaultAudioClientObserver

    private val testObserverFun = { observer: AudioVideoObserver ->
        observer.onAudioClientConnecting(
            false
        )
    }

    private val testId1 = "aliceId"
    private val testId2 = "bobId"
    private val invalidInput = 1000

    private val testAttendeeVolumeUpdate =
        AttendeeUpdate(testId1, testId1, VolumeLevel.High.value)
    private val testAttendeeSignalUpdate =
        AttendeeUpdate(testId1, testId1, SignalStrength.High.value)
    private val testAttendeeVolumeMuted =
        AttendeeUpdate(testId1, testId1, VolumeLevel.Muted.value)
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

    @MockK
    private lateinit var clientMetricsCollector: ClientMetricsCollector

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioClientObserver =
            DefaultAudioClientObserver(
                mockLogger,
                clientMetricsCollector
            )
        audioClientObserver.subscribeToAudioClientStateChange(mockAudioVideoObserver)
        audioClientObserver.subscribeToRealTimeEvents(mockRealtimeObserver)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `notifyAudioClientObserver should notify added observers`() {
        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientConnecting(any()) }
    }

    @Test
    fun `unsubscribeFromAudioClientStateChange should result in no notification`() {
        audioClientObserver.unsubscribeFromAudioClientStateChange(mockAudioVideoObserver)

        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verifyAudioVideoObserverIsNotNotified()
    }

    @Test
    fun `onVolumeStateChange should notify added observers`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChange(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no attendee updates`() {
        audioClientObserver.onVolumeStateChange(null)

        verify(exactly = 0) { mockRealtimeObserver.onVolumeChange(any()) }
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
            mockRealtimeObserver.onVolumeChange(expectedArgs1)
            mockRealtimeObserver.onVolumeChange(expectedArgs2)
        }
    }

    @Test
    fun `onVolumeChange should filter out invalid volume levels`() {
        val testInput = arrayOf(
            testAttendeeVolumeUpdate,
            AttendeeUpdate(testId2, testId2, invalidInput)
        )
        val expectedArgs: Array<VolumeUpdate> = arrayOf(testVolumeUpdate)

        audioClientObserver.onVolumeStateChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onVolumeChange(expectedArgs) }
    }

    @Test
    fun `onSignalStrengthChange should notify added observers`() {
        audioClientObserver.onSignalStrengthChange(arrayOf(testAttendeeSignalUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChange(any()) }
    }

    @Test
    fun `onSignalStrengthChange should NOT notify when no attendee updates`() {
        audioClientObserver.onSignalStrengthChange(null)

        verify(exactly = 0) { mockRealtimeObserver.onSignalStrengthChange(any()) }
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
            mockRealtimeObserver.onSignalStrengthChange(expectedArgs1)
            mockRealtimeObserver.onSignalStrengthChange(expectedArgs2)
        }
    }

    @Test
    fun `onSignalStrengthChange should filter out invalid signal strengths`() {
        val testInput = arrayOf(
            testAttendeeSignalUpdate,
            AttendeeUpdate(testId2, testId2, invalidInput)
        )
        val expectedArgs: Array<SignalUpdate> = arrayOf(testSignalUpdate)

        audioClientObserver.onSignalStrengthChange(testInput)

        verify(exactly = 1) { mockRealtimeObserver.onSignalStrengthChange(expectedArgs) }
    }

    @Test
    fun `onAttendeeJoin should notify when new attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoin(any()) }
    }

    @Test
    fun `onAttendeeJoin should NOT notify when NO new attendees`() {
        audioClientObserver.onVolumeStateChange(emptyArray())

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesJoin(any()) }
    }

    @Test
    fun `onAttendeeLeave should notify when attendees leave`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(emptyArray())

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesLeave(any()) }
    }

    @Test
    fun `onAttendeeJoin should NOT notify when NO attendees leave`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesLeave(any()) }
    }

    @Test
    fun `onAttendeesChange should consider attendee as same when attendeeId and externalUserId are same`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(
            arrayOf(
                AttendeeUpdate(
                    testAttendeeVolumeUpdate.profileId,
                    testAttendeeVolumeUpdate.externalUserId,
                    testAttendeeVolumeUpdate.data
                )
            )
        )

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesJoin(arrayOf(testAttendeeInfo)) }
    }

    @Test
    fun `onAttendeesChange should consider attendee as different when attendeeId is different`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(
            arrayOf(
                AttendeeUpdate(
                    testId2,
                    testAttendeeVolumeUpdate.externalUserId,
                    testAttendeeVolumeUpdate.data
                )
            )
        )

        verifyOrder {
            mockRealtimeObserver.onAttendeesJoin(arrayOf(testAttendeeInfo))
            mockRealtimeObserver.onAttendeesJoin(
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
    fun `onAttendeesChange should consider attendee as different when externalUserId is different`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))
        audioClientObserver.onVolumeStateChange(
            arrayOf(
                AttendeeUpdate(
                    testAttendeeVolumeUpdate.profileId,
                    testId2,
                    testAttendeeVolumeUpdate.data
                )
            )
        )

        verifyOrder {
            mockRealtimeObserver.onAttendeesJoin(arrayOf(testAttendeeInfo))
            mockRealtimeObserver.onAttendeesJoin(
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
    fun `onAttendeeMute should notify newly muted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesMute(any()) }
    }

    @Test
    fun `onAttendeeMute should NOT notify when NO newly muted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesMute(any()) }
    }

    @Test
    fun `onAttendeeUnmute should notify newly unmuted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 1) { mockRealtimeObserver.onAttendeesUnmute(any()) }
    }

    @Test
    fun `onAttendeeUnmute should NOT notify when NO newly unmuted attendees`() {
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))
        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeMuted))

        verify(exactly = 0) { mockRealtimeObserver.onAttendeesUnmute(any()) }
    }

    @Test
    fun `unsubscribeFromRealTimeEvents should result in no notification`() {
        audioClientObserver.unsubscribeFromRealTimeEvents(mockRealtimeObserver)

        audioClientObserver.onVolumeStateChange(arrayOf(testAttendeeVolumeUpdate))

        verify(exactly = 0) { mockRealtimeObserver.onVolumeChange(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesJoin(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesLeave(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesMute(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onAttendeesUnmute(any()) }
        verify(exactly = 0) { mockRealtimeObserver.onSignalStrengthChange(any()) }
    }

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
    fun `onAudioClientStateChange should notify of connect event when finished connecting`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStart(false) }
    }

    @Test
    fun `onAudioClientStateChange should notify of reconnect event when finished reconnecting`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStart(true) }
    }

    @Test
    fun `onAudioClientStateChange should notify of reconnect event when start reconnecting`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStart(true) }
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

        verify(exactly = 1) { mockAudioVideoObserver.onConnectionBecomePoor() }
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

        verify(exactly = 1) { mockAudioVideoObserver.onConnectionRecover() }
    }

    @Test
    fun `onAudioClientStateChange should notify of reconnect cancel event when cancelling reconnect`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientReconnectionCancel() }
    }

    @Test
    fun `onAudioClientStateChange should notify of disconnect event when disconnected`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStop(any()) }
    }

    @Test
    fun `onAudioClientStateChange should notify of disconnect event when failure while connecting`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStop(any()) }
    }

    @Test
    fun `onAudioClientStateChange should notify of disconnect event when failure while reconnecting`() {
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

        verify(exactly = 1) { mockAudioVideoObserver.onAudioClientStop(any()) }
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

    @Test
    fun `onMetrics should call clientMetricsCollector processAudioClientMetrics`() {
        val metrics = mutableMapOf(1 to 2.3, 4 to 5.6)
        audioClientObserver.onMetrics(metrics.keys.toIntArray(), metrics.values.toDoubleArray())

        verify { clientMetricsCollector.processAudioClientMetrics(metrics) }
    }

    private fun verifyAudioVideoObserverIsNotNotified() {
        verify(exactly = 0) { mockAudioVideoObserver.onAudioClientConnecting(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioClientStart(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioClientStop(any()) }
        verify(exactly = 0) { mockAudioVideoObserver.onAudioClientReconnectionCancel() }
        verify(exactly = 0) { mockAudioVideoObserver.onConnectionRecover() }
        verify(exactly = 0) { mockAudioVideoObserver.onConnectionBecomePoor() }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoClientConnecting() }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoClientStart() }
        verify(exactly = 0) { mockAudioVideoObserver.onVideoClientStop(any()) }
    }
}
