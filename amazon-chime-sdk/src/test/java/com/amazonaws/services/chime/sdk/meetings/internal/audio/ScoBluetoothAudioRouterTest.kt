/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ScoBluetoothAudioRouter].
 *
 * **Validates: Invariants 2, 3, 4** from the design document:
 * - Invariant 2: Success callback invocation on connection
 * - Invariant 3: Failure callback invocation on error or timeout
 * - Invariant 4: Cancel clears pending state
 */
class ScoBluetoothAudioRouterTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioManager: AudioManager

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var handler: Handler

    private lateinit var router: ScoBluetoothAudioRouter

    // Captured broadcast receiver for simulating SCO state changes
    private var capturedReceiver: BroadcastReceiver? = null

    // Captured timeout runnable
    private var capturedTimeoutRunnable: Runnable? = null
    private var capturedTimeoutDelay: Long = 0

    // Test callback tracking
    private var successCallbackInvoked = false
    private var successRoute: Int = -1
    private var successDeviceType: MediaDeviceType? = null
    private var failureCallbackInvoked = false

    private val testRoute = 3 // AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
    private val testMediaDevice = MediaDevice(
        label = "Test Bluetooth Device",
        type = MediaDeviceType.AUDIO_BLUETOOTH,
        id = "bt-device-1"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Reset callback tracking
        successCallbackInvoked = false
        successRoute = -1
        successDeviceType = null
        failureCallbackInvoked = false

        // Capture the broadcast receiver registration
        val receiverSlot = slot<BroadcastReceiver>()
        every { context.registerReceiver(capture(receiverSlot), any<IntentFilter>()) } answers {
            capturedReceiver = receiverSlot.captured
            Intent()
        }

        // Capture timeout runnable
        val runnableSlot = slot<Runnable>()
        val delaySlot = slot<Long>()
        every { handler.postDelayed(capture(runnableSlot), capture(delaySlot)) } answers {
            capturedTimeoutRunnable = runnableSlot.captured
            capturedTimeoutDelay = delaySlot.captured
            true
        }
        every { handler.removeCallbacks(any<Runnable>()) } just Runs

        // Create the router
        router = ScoBluetoothAudioRouter(context, audioManager, logger, handler)
    }

    @After
    fun teardown() {
        capturedReceiver = null
        capturedTimeoutRunnable = null
    }

    // ========== Test: SCO CONNECTED triggers success callback ==========
    // **Validates: Invariant 2 - Success callback invocation on connection**
    // _Requirements: 7.1_

    @Test
    fun `routeToBluetoothDevice should invoke success callback when SCO state transitions to CONNECTED`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { route, deviceType ->
                successCallbackInvoked = true
                successRoute = route
                successDeviceType = deviceType
            },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: SCO state transitions to CONNECTED
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_CONNECTED,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTING
        )

        // Then: Success callback should be invoked with correct parameters
        assertTrue("Success callback should be invoked", successCallbackInvoked)
        assertEquals("Route should match", testRoute, successRoute)
        assertEquals("Device type should match", MediaDeviceType.AUDIO_BLUETOOTH, successDeviceType)
        assertFalse("Failure callback should not be invoked", failureCallbackInvoked)
    }

    // ========== Test: SCO ERROR triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation on error**
    // _Requirements: 7.2_

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when SCO state transitions to ERROR`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: SCO state transitions to ERROR
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_ERROR,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTING
        )

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: SCO DISCONNECTED from CONNECTING triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation on connection failure**
    // _Requirements: 7.2_

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when SCO transitions from CONNECTING to DISCONNECTED`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: SCO state transitions from CONNECTING to DISCONNECTED (connection attempt failed)
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_DISCONNECTED,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTING
        )

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    @Test
    fun `routeToBluetoothDevice should NOT invoke failure callback when SCO transitions from CONNECTED to DISCONNECTED`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: SCO state transitions from CONNECTED to DISCONNECTED (expected during device switching)
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_DISCONNECTED,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTED
        )

        // Then: Neither callback should be invoked (waiting for new device to connect)
        assertFalse("Failure callback should not be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: Timeout triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation on timeout**
    // _Requirements: 7.6_

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when timeout expires`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // Verify timeout was scheduled with correct delay
        assertEquals(
            "Timeout should be scheduled with SCO_CONNECTION_TIMEOUT_MS",
            BluetoothRoutingConfig.SCO_CONNECTION_TIMEOUT_MS,
            capturedTimeoutDelay
        )

        // When: Timeout expires
        capturedTimeoutRunnable?.run()

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: Cancel prevents callbacks ==========
    // **Validates: Invariant 4 - Cancel clears pending state**
    // _Requirements: 7.6_

    @Test
    fun `cancelPendingOperation should prevent success callback from being invoked`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: Operation is cancelled before SCO connects
        router.cancelPendingOperation()

        // And: SCO state transitions to CONNECTED
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_CONNECTED,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTING
        )

        // Then: Neither callback should be invoked
        assertFalse("Success callback should not be invoked after cancel", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked after cancel", failureCallbackInvoked)
    }

    @Test
    fun `cancelPendingOperation should prevent failure callback from being invoked`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: Operation is cancelled before SCO error
        router.cancelPendingOperation()

        // And: SCO state transitions to ERROR
        simulateScoStateChange(
            state = AudioManager.SCO_AUDIO_STATE_ERROR,
            previousState = AudioManager.SCO_AUDIO_STATE_CONNECTING
        )

        // Then: Neither callback should be invoked
        assertFalse("Success callback should not be invoked after cancel", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked after cancel", failureCallbackInvoked)
    }

    @Test
    fun `cancelPendingOperation should remove timeout callback from handler`() {
        // Given: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> },
            onFailure = { }
        )

        // When: Operation is cancelled
        router.cancelPendingOperation()

        // Then: Timeout callback should be removed from handler
        verify { handler.removeCallbacks(any<Runnable>()) }
    }

    // ========== Test: routeToBluetoothDevice starts SCO ==========

    @Test
    fun `routeToBluetoothDevice should call startBluetoothSco on audioManager`() {
        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> },
            onFailure = { }
        )

        // Then: AudioManager should be configured for Bluetooth SCO
        verify { audioManager.mode = AudioManager.MODE_IN_COMMUNICATION }
        verify { audioManager.isSpeakerphoneOn = false }
        verify { audioManager.startBluetoothSco() }
        verify { audioManager.isBluetoothScoOn = true }
    }

    // ========== Helper methods ==========

    /**
     * Simulates an SCO state change by invoking the captured broadcast receiver.
     */
    private fun simulateScoStateChange(state: Int, previousState: Int) {
        val intent = mockk<Intent>()
        every { intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) } returns state
        every { intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1) } returns previousState

        capturedReceiver?.onReceive(context, intent)
    }
}
