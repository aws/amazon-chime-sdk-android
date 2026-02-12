/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.media.AudioDeviceInfo
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
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CommunicationDeviceBluetoothAudioRouter].
 *
 * **Validates: Invariants 2, 3, 4, 5** from the design document:
 * - Invariant 2: Success callback invocation on connection
 * - Invariant 3: Failure callback invocation on error or timeout
 * - Invariant 4: Cancel clears pending state
 * - Invariant 5: Retry-on-mismatch behavior (API 31+)
 */
class CommunicationDeviceBluetoothAudioRouterTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioManager: AudioManager

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var handler: Handler

    @MockK
    private lateinit var mainExecutor: Executor

    private lateinit var router: CommunicationDeviceBluetoothAudioRouter

    // Captured communication device listener for simulating device changes
    private var capturedListener: AudioManager.OnCommunicationDeviceChangedListener? = null

    // Captured timeout runnable
    private var capturedTimeoutRunnable: Runnable? = null
    private var capturedTimeoutDelay: Long = 0

    // Captured retry runnable
    private var capturedRetryRunnable: Runnable? = null
    private var capturedRetryDelay: Long = 0

    // Test callback tracking
    private var successCallbackInvoked = false
    private var successRoute: Int = -1
    private var successDeviceType: MediaDeviceType? = null
    private var failureCallbackInvoked = false

    private val testRoute = 3 // AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
    private val testDeviceId = 42
    private val testMediaDevice = MediaDevice(
        label = "Test Bluetooth Device",
        type = MediaDeviceType.AUDIO_BLUETOOTH,
        id = testDeviceId.toString()
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Reset callback tracking
        successCallbackInvoked = false
        successRoute = -1
        successDeviceType = null
        failureCallbackInvoked = false
        capturedRetryRunnable = null
        capturedRetryDelay = 0

        // Mock context.mainExecutor
        every { context.mainExecutor } returns mainExecutor

        // Capture the communication device listener registration
        val listenerSlot = slot<AudioManager.OnCommunicationDeviceChangedListener>()
        every { audioManager.addOnCommunicationDeviceChangedListener(any(), capture(listenerSlot)) } answers {
            capturedListener = listenerSlot.captured
        }
        every { audioManager.removeOnCommunicationDeviceChangedListener(any()) } just Runs

        // Capture timeout runnable
        val runnableSlot = slot<Runnable>()
        val delaySlot = slot<Long>()
        every { handler.postDelayed(capture(runnableSlot), capture(delaySlot)) } answers {
            val delay = delaySlot.captured
            if (delay == BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS) {
                capturedTimeoutRunnable = runnableSlot.captured
                capturedTimeoutDelay = delay
            } else if (delay == BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS) {
                capturedRetryRunnable = runnableSlot.captured
                capturedRetryDelay = delay
            }
            true
        }
        every { handler.removeCallbacks(any<Runnable>()) } just Runs

        // Create the router
        router = CommunicationDeviceBluetoothAudioRouter(context, audioManager, logger, handler)
    }

    @After
    fun teardown() {
        capturedListener = null
        capturedTimeoutRunnable = null
        capturedRetryRunnable = null
    }

    // ========== Test: Bluetooth device confirmed triggers success callback ==========
    // **Validates: Invariant 2 - Success callback invocation on connection**
    // _Requirements: 7.3_

    @Test
    fun `routeToBluetoothDevice should invoke success callback when Bluetooth SCO device is confirmed`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
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

        // And: Communication device changes to Bluetooth SCO
        simulateCommunicationDeviceChanged(bluetoothDevice)

        // Then: Success callback should be invoked with correct parameters
        assertTrue("Success callback should be invoked", successCallbackInvoked)
        assertEquals("Route should match", testRoute, successRoute)
        assertEquals("Device type should match", MediaDeviceType.AUDIO_BLUETOOTH, successDeviceType)
        assertFalse("Failure callback should not be invoked", failureCallbackInvoked)
    }

    // ========== Test: Device mismatch triggers retry ==========
    // **Validates: Invariant 5 - Retry-on-mismatch behavior (API 31+)**
    // _Requirements: 7.4_

    @Test
    fun `routeToBluetoothDevice should retry when a different Bluetooth SCO device is confirmed`() {
        // Given: Two Bluetooth devices are available
        val targetDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val otherBtDevice = createMockAudioDeviceInfo(77, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(targetDevice, otherBtDevice)
        every { audioManager.setCommunicationDevice(targetDevice) } returns true

        // When: Routing to target Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Communication device changes to a DIFFERENT Bluetooth SCO device
        simulateCommunicationDeviceChanged(otherBtDevice)

        // Then: Retry should be scheduled (not success)
        assertEquals(
            "Retry should be scheduled with correct delay",
            BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS,
            capturedRetryDelay
        )
        assertFalse("Success callback should not be invoked for wrong BT device", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked yet", failureCallbackInvoked)
    }

    @Test
    fun `routeToBluetoothDevice should schedule retry when device mismatch occurs`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val handsetDevice = createMockAudioDeviceInfo(99, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice, handsetDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Communication device changes to Handset instead of Bluetooth (mismatch)
        simulateCommunicationDeviceChanged(handsetDevice)

        // Then: Retry should be scheduled
        assertEquals(
            "Retry should be scheduled with correct delay",
            BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS,
            capturedRetryDelay
        )
        assertFalse("Success callback should not be invoked yet", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked yet", failureCallbackInvoked)
    }

    @Test
    fun `retry should call setCommunicationDevice again when device is still available`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val handsetDevice = createMockAudioDeviceInfo(99, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice, handsetDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true
        every { audioManager.clearCommunicationDevice() } just Runs

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Communication device changes to Handset (mismatch)
        simulateCommunicationDeviceChanged(handsetDevice)

        // And: Retry runnable executes
        capturedRetryRunnable?.run()

        // Then: clearCommunicationDevice should be called before retrying
        verify(exactly = 1) { audioManager.clearCommunicationDevice() }
        // And: setCommunicationDevice should be called again
        verify(exactly = 2) { audioManager.setCommunicationDevice(bluetoothDevice) }
    }

    // ========== Test: Max retries exceeded triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation after max retries**
    // _Requirements: 7.5_

    @Test
    fun `routeToBluetoothDevice should invoke failure callback after max retries exceeded`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val handsetDevice = createMockAudioDeviceInfo(99, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice, handsetDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Device mismatch occurs MAX_RETRIES + 1 times
        repeat(BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES + 1) {
            simulateCommunicationDeviceChanged(handsetDevice)
            if (capturedRetryRunnable != null) {
                capturedRetryRunnable?.run()
                capturedRetryRunnable = null
            }
        }

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked after max retries", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: Device unavailable on retry triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation when device unavailable**
    // _Requirements: 7.5_

    @Test
    fun `retry should invoke failure callback when device is no longer available`() {
        // Given: A Bluetooth device is initially available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val handsetDevice = createMockAudioDeviceInfo(99, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice, handsetDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Communication device changes to Handset (mismatch)
        simulateCommunicationDeviceChanged(handsetDevice)

        // And: Bluetooth device becomes unavailable before retry
        every { audioManager.availableCommunicationDevices } returns listOf(handsetDevice)

        // And: Retry runnable executes
        capturedRetryRunnable?.run()

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked when device unavailable", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: Timeout triggers failure callback ==========
    // **Validates: Invariant 3 - Failure callback invocation on timeout**
    // _Requirements: 7.6_

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when timeout expires`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // Verify timeout was scheduled with correct delay
        assertEquals(
            "Timeout should be scheduled with COMMUNICATION_DEVICE_TIMEOUT_MS",
            BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS,
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
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // And: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: Operation is cancelled before device confirmation
        router.cancelPendingOperation()

        // And: Communication device changes to Bluetooth SCO
        simulateCommunicationDeviceChanged(bluetoothDevice)

        // Then: Neither callback should be invoked
        assertFalse("Success callback should not be invoked after cancel", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked after cancel", failureCallbackInvoked)
    }

    @Test
    fun `cancelPendingOperation should prevent failure callback from being invoked on timeout`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // And: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // When: Operation is cancelled before timeout
        router.cancelPendingOperation()

        // And: Timeout expires
        capturedTimeoutRunnable?.run()

        // Then: Neither callback should be invoked
        assertFalse("Success callback should not be invoked after cancel", successCallbackInvoked)
        assertFalse("Failure callback should not be invoked after cancel", failureCallbackInvoked)
    }

    @Test
    fun `cancelPendingOperation should remove timeout and retry callbacks from handler`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // And: A pending Bluetooth routing operation
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> },
            onFailure = { }
        )

        // When: Operation is cancelled
        router.cancelPendingOperation()

        // Then: Callbacks should be removed from handler
        verify(atLeast = 1) { handler.removeCallbacks(any<Runnable>()) }
    }

    // ========== Test: setCommunicationDevice failure invokes failure callback ==========

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when setCommunicationDevice returns false`() {
        // Given: A Bluetooth device is available but setCommunicationDevice fails
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns false

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // Then: Failure callback should be invoked immediately
        assertTrue("Failure callback should be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    @Test
    fun `routeToBluetoothDevice should invoke failure callback when device not found`() {
        // Given: No Bluetooth device is available
        every { audioManager.availableCommunicationDevices } returns emptyList()

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // Then: Failure callback should be invoked immediately
        assertTrue("Failure callback should be invoked", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Test: Retry setCommunicationDevice failure invokes failure callback ==========

    @Test
    fun `retry should invoke failure callback when setCommunicationDevice returns false`() {
        // Given: A Bluetooth device is available
        val bluetoothDevice = createMockAudioDeviceInfo(testDeviceId, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val handsetDevice = createMockAudioDeviceInfo(99, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        every { audioManager.availableCommunicationDevices } returns listOf(bluetoothDevice, handsetDevice)
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns true

        // When: Routing to Bluetooth device
        router.routeToBluetoothDevice(
            mediaDevice = testMediaDevice,
            route = testRoute,
            onSuccess = { _, _ -> successCallbackInvoked = true },
            onFailure = { failureCallbackInvoked = true }
        )

        // And: Communication device changes to Handset (mismatch)
        simulateCommunicationDeviceChanged(handsetDevice)

        // And: setCommunicationDevice will fail on retry
        every { audioManager.setCommunicationDevice(bluetoothDevice) } returns false

        // And: Retry runnable executes
        capturedRetryRunnable?.run()

        // Then: Failure callback should be invoked
        assertTrue("Failure callback should be invoked when retry fails", failureCallbackInvoked)
        assertFalse("Success callback should not be invoked", successCallbackInvoked)
    }

    // ========== Helper methods ==========

    /**
     * Creates a mock AudioDeviceInfo with the specified ID and type.
     */
    private fun createMockAudioDeviceInfo(id: Int, type: Int): AudioDeviceInfo {
        val device = mockk<AudioDeviceInfo>()
        every { device.id } returns id
        every { device.type } returns type
        every { device.productName } returns "Mock Device $id"
        return device
    }

    /**
     * Simulates a communication device change by invoking the captured listener.
     */
    private fun simulateCommunicationDeviceChanged(device: AudioDeviceInfo?) {
        capturedListener?.onCommunicationDeviceChanged(device)
    }
}
