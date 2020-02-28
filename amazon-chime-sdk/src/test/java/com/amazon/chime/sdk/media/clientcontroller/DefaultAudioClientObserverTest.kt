/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.SessionStateControllerAction
import com.amazon.chime.sdk.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultAudioClientObserverTest {
    @MockK
    private lateinit var mockLogger: Logger

    private lateinit var audioClientObserver: DefaultAudioClientObserver

    private var observerCalled = 0

    @MockK
    private lateinit var audioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var realtimeObserver: RealtimeObserver

    private val testObserverFun = { observer: AudioVideoObserver ->
        observer.onAudioVideoStartConnecting(
            false
        )
    }

    private val testProfileIds = arrayOf("aliceId", "bobId")

    // Used for both volume and signal
    private val testValues = IntArray(2) { it }
    private val newTestValues = IntArray(2) { it * 2 }

    private val mockUIThread = newSingleThreadContext("UI thread")

    @MockK
    private lateinit var clientMetricsCollector: ClientMetricsCollector

    @Before
    fun setup() {
        observerCalled = 0
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioClientObserver = DefaultAudioClientObserver(mockLogger, clientMetricsCollector)
        audioClientObserver.subscribeToAudioClientStateChange(audioVideoObserver)
        audioClientObserver.subscribeToRealTimeEvents(realtimeObserver)
        Dispatchers.setMain(mockUIThread)
    }

    @After
    fun tearDown() {
        mockUIThread.close()
    }

    @Test
    fun `notifyAudioClientObserver should notify added observers`() {
        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verify(exactly = 1) { audioVideoObserver.onAudioVideoStartConnecting(any()) }
    }

    @Test
    fun `unsubscribeFromAudioClientStateChange should result in no notification`() {
        audioClientObserver.unsubscribeFromAudioClientStateChange(audioVideoObserver)

        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        verifyAudioVideoObserverIsNotNotified()
    }

    @Test
    fun `onVolumeStateChange should notify added observers`() {
        audioClientObserver.onVolumeStateChange(testProfileIds, testValues)

        verify(exactly = 1) { realtimeObserver.onVolumeChange(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no profile Ids`() {
        audioClientObserver.onVolumeStateChange(null, testValues)

        verify(exactly = 0) { realtimeObserver.onVolumeChange(any()) }
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no volumes`() {
        audioClientObserver.onVolumeStateChange(testProfileIds, null)

        verify(exactly = 0) { realtimeObserver.onVolumeChange(any()) }
    }

    @Test
    fun `onVolumeStateChange should only notify delta`() {
        audioClientObserver.onVolumeStateChange(testProfileIds, testValues)
        audioClientObserver.onVolumeStateChange(testProfileIds, newTestValues)

        val expectedArgs1 = mutableMapOf(
            testProfileIds[0] to VolumeLevel.from(testValues[0])!!,
            testProfileIds[1] to VolumeLevel.from(testValues[1])!!
        )
        val expectedArgs2 = mutableMapOf(testProfileIds[1] to VolumeLevel.from(newTestValues[1])!!)
        verifyOrder {
            realtimeObserver.onVolumeChange(expectedArgs1)
            realtimeObserver.onVolumeChange(expectedArgs2)
        }
    }

    @Test
    fun `onSignalStrengthChange should notify added observers`() {
        audioClientObserver.onSignalStrengthChange(testProfileIds, testValues)

        verify(exactly = 1) { realtimeObserver.onSignalStrengthChange(any()) }
    }

    @Test
    fun `onSignalStrengthChange should notify when no profile Ids`() {
        audioClientObserver.onSignalStrengthChange(null, testValues)

        verify(exactly = 0) { realtimeObserver.onSignalStrengthChange(any()) }
    }

    @Test
    fun `onSignalStrengthChange should notify when no volumes`() {
        audioClientObserver.onSignalStrengthChange(testProfileIds, null)

        verify(exactly = 0) { realtimeObserver.onSignalStrengthChange(any()) }
    }

    @Test
    fun `onSignalStrengthChange should only notify delta`() {
        audioClientObserver.onSignalStrengthChange(testProfileIds, testValues)
        audioClientObserver.onSignalStrengthChange(testProfileIds, newTestValues)

        val expectedArgs1 = mutableMapOf(
            testProfileIds[0] to SignalStrength.from(testValues[0])!!,
            testProfileIds[1] to SignalStrength.from(testValues[1])!!
        )
        val expectedArgs2 = mutableMapOf(testProfileIds[1] to SignalStrength.from(newTestValues[1])!!)
        verifyOrder {
            realtimeObserver.onSignalStrengthChange(expectedArgs1)
            realtimeObserver.onSignalStrengthChange(expectedArgs2)
        }
    }

    @Test
    fun `unsubscribeFromRealTimeEvents should result in no notification`() {
        audioClientObserver.unsubscribeFromRealTimeEvents(realtimeObserver)

        audioClientObserver.onVolumeStateChange(testProfileIds, testValues)

        verify(exactly = 0) { realtimeObserver.onVolumeChange(any()) }
        verify(exactly = 0) { realtimeObserver.onAttendeesJoin(any()) }
        verify(exactly = 0) { realtimeObserver.onAttendeesLeave(any()) }
        verify(exactly = 0) { realtimeObserver.onAttendeesMute(any()) }
        verify(exactly = 0) { realtimeObserver.onAttendeesUnmute(any()) }
        verify(exactly = 0) { realtimeObserver.onSignalStrengthChange(any()) }
    }

    @Test
    fun `onAudioClientStateChange should NOT notify when new state is UNKNOWN`() {
        audioClientObserver.onAudioClientStateChange(
            100,
            100
        )

        verifyAudioVideoObserverIsNotNotified()
    }

    @Test
    fun `onAudioClientStateChange should NOT notify when state and status is unchanged`() {
        audioClientObserver.onAudioClientStateChange(
            SessionStateControllerAction.Init.value,
            MeetingSessionStatusCode.OK.value
        )

        verifyAudioVideoObserverIsNotNotified()
    }

    @Test
    fun `onMetrics should call clientMetricsCollector processAudioClientMetrics`() {
        val metrics = mutableMapOf(1 to 2.3, 4 to 5.6)
        audioClientObserver.onMetrics(metrics.keys.toIntArray(), metrics.values.toDoubleArray())

        verify { clientMetricsCollector.processAudioClientMetrics(metrics) }
    }

    private fun verifyAudioVideoObserverIsNotNotified() {
        verify(exactly = 0) { audioVideoObserver.onAudioVideoStartConnecting(any()) }
        verify(exactly = 0) { audioVideoObserver.onAudioVideoStart(any()) }
        verify(exactly = 0) { audioVideoObserver.onAudioVideoStop(any()) }
        verify(exactly = 0) { audioVideoObserver.onAudioReconnectionCancel() }
        verify(exactly = 0) { audioVideoObserver.onConnectionBecamePoor() }
        verify(exactly = 0) { audioVideoObserver.onConnectionRecovered() }
        verify(exactly = 0) { audioVideoObserver.onMetricsReceive(any()) }
    }
}
