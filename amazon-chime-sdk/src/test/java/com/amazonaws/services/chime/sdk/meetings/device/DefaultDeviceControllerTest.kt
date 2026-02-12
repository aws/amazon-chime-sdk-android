/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientState
import com.amazonaws.services.chime.sdk.meetings.internal.audio.BluetoothAudioRouter
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.MediaError
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultDeviceControllerTest {
    @MockK
    private lateinit var activeConfiguration: AudioRecordingConfiguration

    @MockK
    private lateinit var audioDevice: AudioDeviceInfo

    @MockK
    private lateinit var speakerInfo: AudioDeviceInfo

    @MockK
    private lateinit var earpieceInfo: AudioDeviceInfo

    @MockK
    private lateinit var telephonyInfo: AudioDeviceInfo

    @MockK
    private lateinit var wiredHeadsetInfo: AudioDeviceInfo

    @MockK
    private lateinit var bluetoothInfo: AudioDeviceInfo

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var eventAnalyticsController: EventAnalyticsController

    @MockK
    private lateinit var audioManager: AudioManager

    @MockK
    private lateinit var deviceChangeObserver: DeviceChangeObserver

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var bluetoothInfo2: AudioDeviceInfo

    @MockK
    private lateinit var mockBluetoothAudioRouter: BluetoothAudioRouter

    private lateinit var deviceController: DefaultDeviceController

    private val testDispatcher = TestCoroutineDispatcher()

    // Captured callbacks for Bluetooth routing tests
    private var capturedOnSuccess: ((Int, MediaDeviceType) -> Unit)? = null
    private var capturedOnFailure: (() -> Unit)? = null

    private fun setupForNewAPILevel() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.registerReceiver(any(), any()) } returns Intent()
        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            eventAnalyticsController,
            mockLogger,
            audioManager,
            24,
            mockBluetoothAudioRouter
        )
        commonSetup()
    }

    private fun setupForOldAPILevel() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.registerReceiver(any(), any()) } returns Intent()
        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            eventAnalyticsController,
            mockLogger,
            audioManager,
            21,
            mockBluetoothAudioRouter
        )
        commonSetup()
    }

    /**
     * Sets up the test environment for API 31+ (Android 12+) tests.
     */
    private fun setupForAPI31Plus(apiLevel: Int = 31) {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.registerReceiver(any(), any()) } returns Intent()

        // Capture Bluetooth routing callbacks
        val onSuccessSlot = slot<(Int, MediaDeviceType) -> Unit>()
        val onFailureSlot = slot<() -> Unit>()
        every {
            mockBluetoothAudioRouter.routeToBluetoothDevice(any(), any(), capture(onSuccessSlot), capture(onFailureSlot))
        } answers {
            capturedOnSuccess = onSuccessSlot.captured
            capturedOnFailure = onFailureSlot.captured
        }

        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            eventAnalyticsController,
            mockLogger,
            audioManager,
            apiLevel,
            mockBluetoothAudioRouter
        )
        commonSetupForBluetooth()
    }

    /**
     * Sets up the test environment for API < 31 (Android 11 and below) tests.
     */
    private fun setupForAPI30AndBelow(apiLevel: Int = 30) {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.registerReceiver(any(), any()) } returns Intent()

        // Capture Bluetooth routing callbacks
        val onSuccessSlot = slot<(Int, MediaDeviceType) -> Unit>()
        val onFailureSlot = slot<() -> Unit>()
        every {
            mockBluetoothAudioRouter.routeToBluetoothDevice(any(), any(), capture(onSuccessSlot), capture(onFailureSlot))
        } answers {
            capturedOnSuccess = onSuccessSlot.captured
            capturedOnFailure = onFailureSlot.captured
        }

        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            eventAnalyticsController,
            mockLogger,
            audioManager,
            apiLevel,
            mockBluetoothAudioRouter
        )
        commonSetupForBluetooth()
    }

    private fun commonSetupForBluetooth() {
        every { speakerInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerInfo.productName } returns "Speaker"
        every { speakerInfo.id } returns 1
        every { earpieceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        every { earpieceInfo.productName } returns "Handset"
        every { earpieceInfo.id } returns 2
        every { wiredHeadsetInfo.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetInfo.productName } returns "Wired Headset"
        every { wiredHeadsetInfo.id } returns 3
        every { bluetoothInfo.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothInfo.productName } returns "Bluetooth Headset"
        every { bluetoothInfo.id } returns 4
        every { bluetoothInfo2.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothInfo2.productName } returns "Bluetooth Headset 2"
        every { bluetoothInfo2.id } returns 5

        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, earpieceInfo, bluetoothInfo
        )
        every { audioManager.availableCommunicationDevices } returns listOf(
            speakerInfo, earpieceInfo, bluetoothInfo
        )
        every { audioManager.setCommunicationDevice(any()) } returns true
        every { audioManager.clearCommunicationDevice() } just Runs
        every { audioManager.activeRecordingConfigurations } returns emptyList()

        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        every { audioClientController.setRoute(any()) } returns true
    }

    private fun commonSetup() {
        every { speakerInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerInfo.productName } returns "default speaker"
        every { speakerInfo.id } returns 1
        every { earpieceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        every { earpieceInfo.productName } returns "default receiver"
        every { earpieceInfo.id } returns 2
        every { telephonyInfo.type } returns AudioDeviceInfo.TYPE_TELEPHONY
        every { telephonyInfo.productName } returns "telephony receiver"
        every { telephonyInfo.id } returns 3
        every { wiredHeadsetInfo.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetInfo.productName } returns "my wired headset"
        every { wiredHeadsetInfo.id } returns 4
        every { bluetoothInfo.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothInfo.productName } returns "my bluetooth headphone"
        every { bluetoothInfo.id } returns 5
        every { audioDevice.productName } returns "my product name"
        every { audioDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { audioDevice.id } returns 6
        every { activeConfiguration.audioDevice } returns audioDevice
        every { audioManager.activeRecordingConfigurations } returns listOf(activeConfiguration)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        capturedOnSuccess = null
        capturedOnFailure = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        unmockkAll()
    }

    @Test
    fun `deviceController should register device change event when build version is high`() {
        setupForNewAPILevel()
        verify { audioManager.registerAudioDeviceCallback(any(), null) }
    }

    @Test
    fun `deviceController should call BluetoothDeviceController startListening`() {
        setupForNewAPILevel()
    }

    @Test
    fun `deviceController stopListening should call BluetoothDeviceController stopListening`() {
        setupForNewAPILevel()
    }

    @Test
    fun `deviceController getActiveAudioDevice should return device from audioManager active recording`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, earpieceInfo, audioDevice
        )
        val expected = MediaDevice("my product name (Bluetooth)", MediaDeviceType.AUDIO_BLUETOOTH, id = "6")
        val mediaDevice = deviceController.getActiveAudioDevice()
        assertEquals(expected, mediaDevice)
    }

    @Test
    fun `deviceController should register device change event when build version is low`() {
        setupForOldAPILevel()
        verify(exactly = 3) { context.registerReceiver(any(), any()) }
    }

    @Test
    fun `listAudioDevices should return a list of connected devices with product name when build version is high`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, earpieceInfo, bluetoothInfo
        )

        val devices = deviceController.listAudioDevices()

        assertEquals(3, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER &&
                        it.label == "default speaker (Speaker)" ||
                        it.type == MediaDeviceType.AUDIO_HANDSET &&
                        it.label == "default receiver (Handset)" ||
                        it.type == MediaDeviceType.AUDIO_BLUETOOTH &&
                        it.label == "my bluetooth headphone (Bluetooth)"
            )
        }
    }

    @Test
    fun `listAudioDevices should public audioInputFailed event when no device available`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf()

        deviceController.listAudioDevices()

        val attributes = mutableMapOf<EventAttributeName, Any>(
            EventAttributeName.audioInputErrorMessage to MediaError.NoAudioDevices
        )
        verify(exactly = 1) { eventAnalyticsController.publishEvent(EventName.audioInputFailed, attributes) }
    }

    @Test
    fun `listAudioDevices should return a list of connected devices when build version is low`() {
        setupForOldAPILevel()
        every { audioManager.isBluetoothScoOn } returns true
        every { audioManager.isBluetoothA2dpOn } returns true
        every { audioManager.isWiredHeadsetOn } returns false

        val devices = deviceController.listAudioDevices()
        assertEquals(3, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_HANDSET &&
                        it.label == "Handset" ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER &&
                        it.label == "Speaker" ||
                        it.type == MediaDeviceType.AUDIO_BLUETOOTH &&
                        it.label == "Bluetooth"
            )
        }
    }

    @Test
    fun `listAudioDevices should not return both wired headset and receiver when build version is high`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, earpieceInfo, wiredHeadsetInfo
        )

        val devices = deviceController.listAudioDevices()
        assertEquals(2, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_WIRED_HEADSET ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        }
    }

    @Test
    fun `listAudioDevices should return one handset when multiple handset available (only apply to higher API level)`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            earpieceInfo, telephonyInfo
        )

        val devices = deviceController.listAudioDevices()
        assertEquals(1, devices.size)
        assertEquals(MediaDeviceType.AUDIO_HANDSET, devices[0].type)
    }

    @Test
    fun `listAudioDevices should not return both wired headset and receiver when build version is low`() {
        setupForOldAPILevel()
        every { audioManager.isBluetoothScoOn } returns false
        every { audioManager.isBluetoothA2dpOn } returns false
        every { audioManager.isWiredHeadsetOn } returns true

        val devices = deviceController.listAudioDevices()
        assertEquals(2, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_WIRED_HEADSET ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        }
    }

    @Test
    fun `chooseAudioDevice should call AudioClientController setRoute`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(
            MediaDevice(
                "speaker",
                MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        )

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
    }

    @Test
    fun `chooseAudioDevice should call AudioClientController setRoute with headset`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(
            MediaDevice(
                "usb headset",
                MediaDeviceType.AUDIO_USB_HEADSET
            )
        )

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_HEADSET) }
    }

    @Test
    fun `chooseAudioDevice should delegate to BluetoothAudioRouter when choosing bluetooth device`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        val bluetoothDevice = MediaDevice("bluetooth", MediaDeviceType.AUDIO_BLUETOOTH)

        deviceController.chooseAudioDevice(bluetoothDevice)

        verify { mockBluetoothAudioRouter.routeToBluetoothDevice(bluetoothDevice, AudioClient.SPK_STREAM_ROUTE_BT_AUDIO, any(), any()) }
    }

    @Test
    fun `chooseAudioDevice should disable speaker and bluetooth when choosing other devices`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(
            MediaDevice(
                "wired headset",
                MediaDeviceType.AUDIO_WIRED_HEADSET
            )
        )

        verify { audioManager.setSpeakerphoneOn(false) }
        verify { audioManager.setBluetoothScoOn(false) }
    }

    @Test
    fun `chooseAudioDevice should default to handset when not bluetooth, wired headset, or speaker`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(
            MediaDevice(
                "handset",
                MediaDeviceType.AUDIO_HANDSET
            )
        )

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_RECEIVER) }
    }

    @Test
    fun `chooseAudioDevice should call publishEvent when setRoute`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(
            MediaDevice(
                "usb headset",
                MediaDeviceType.AUDIO_USB_HEADSET
            )
        )

        verify { eventAnalyticsController.publishEvent(EventName.audioInputSelected, mutableMapOf(
            EventAttributeName.audioDeviceType to MediaDeviceType.AUDIO_USB_HEADSET.toString()
        ), false) }
    }

    @Test
    fun `deviceController should call publishEvent when audio device is selected`() {
        setupForNewAPILevel()
        every { audioClientController.setRoute(any()) } returns true
        mockkStatic(DefaultAudioClientController::class)
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        deviceController.chooseAudioDevice(MediaDevice(
            "speaker",
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER
        ))

        verify(exactly = 1) {
            eventAnalyticsController.publishEvent(EventName.audioInputSelected, mutableMapOf(
                EventAttributeName.audioDeviceType to MediaDeviceType.AUDIO_BUILTIN_SPEAKER.toString()
            ), false)
        }
    }

    @Test
    fun `getActiveCamera should return null when no active camera`() {
        setupForOldAPILevel()
        every { videoClientController.getActiveCamera() } returns null

        assertNull(deviceController.getActiveCamera())
    }

    @Test
    fun `getActiveCamera should return a media device when active camera existing`() {
        setupForOldAPILevel()
        val videoDevice = mockkClass(MediaDevice::class)
        every { videoDevice.id } returns "0"
        every { videoDevice.type } returns MediaDeviceType.VIDEO_FRONT_CAMERA
        every { videoClientController.getActiveCamera() } returns videoDevice

        val mediaDevice = deviceController.getActiveCamera()!!

        assertEquals("0", mediaDevice.id)
        assertEquals(MediaDeviceType.VIDEO_FRONT_CAMERA, mediaDevice.type)
    }

    @Test
    fun `switchCamera should call videoClientController switchCamera`() {
        setupForOldAPILevel()

        deviceController.switchCamera()

        verify { videoClientController.switchCamera() }
    }

    @Test
    fun `notifyAudioDeviceChange should notify added observers`() {
        setupForOldAPILevel()
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        every { audioManager.isBluetoothScoOn } returns false
        every { audioManager.isBluetoothA2dpOn } returns false
        every { audioManager.isWiredHeadsetOn } returns false

        deviceController.notifyAudioDeviceChange()

        verify { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }

    @Test
    fun `notifyAudioDeviceChange should NOT notify removed observer`() {
        setupForOldAPILevel()
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        deviceController.removeDeviceChangeObserver(deviceChangeObserver)

        deviceController.notifyAudioDeviceChange()

        verify(exactly = 0) { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }

    @Test
    fun `getActiveAudioDevice should return null for old API Level`() {
        setupForOldAPILevel()
        val mediaDevice = deviceController.getActiveAudioDevice()
        assertNull(mediaDevice)
    }

    @Test
    fun `deviceController should call BluetoothDeviceController startListening for old API level`() {
        setupForOldAPILevel()
    }

    @Test
    fun `deviceController stopListening should call BluetoothDeviceController stopListening for old API level`() {
        setupForOldAPILevel()
    }

    // ==================== Bluetooth Routing Delegation Tests - API 31+ ====================

    @Test
    fun `chooseAudioDevice should immediately call setRoute for speaker on API 31+`() {
        setupForAPI31Plus(31)
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER, id = "1")

        deviceController.chooseAudioDevice(speakerDevice)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
    }

    @Test
    fun `chooseAudioDevice should immediately call setRoute for wired headset on API 31+`() {
        setupForAPI31Plus(31)
        every { audioManager.availableCommunicationDevices } returns listOf(
            speakerInfo, earpieceInfo, bluetoothInfo, wiredHeadsetInfo
        )
        val wiredDevice = MediaDevice("Wired Headset", MediaDeviceType.AUDIO_WIRED_HEADSET, id = "3")

        deviceController.chooseAudioDevice(wiredDevice)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_HEADSET) }
    }

    @Test
    fun `chooseAudioDevice should immediately call setRoute for handset on API 31+`() {
        setupForAPI31Plus(31)
        val handsetDevice = MediaDevice("Handset", MediaDeviceType.AUDIO_HANDSET, id = "2")

        deviceController.chooseAudioDevice(handsetDevice)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_RECEIVER) }
    }

    @Test
    fun `chooseAudioDevice should delegate to BluetoothAudioRouter for Bluetooth on API 31+`() {
        setupForAPI31Plus(31)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH, id = "4")

        deviceController.chooseAudioDevice(bluetoothDevice)

        verify { mockBluetoothAudioRouter.routeToBluetoothDevice(bluetoothDevice, AudioClient.SPK_STREAM_ROUTE_BT_AUDIO, any(), any()) }
    }

    @Test
    fun `chooseAudioDevice should not call setRoute directly for Bluetooth on API 31+`() {
        setupForAPI31Plus(31)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH, id = "4")

        deviceController.chooseAudioDevice(bluetoothDevice)

        verify(exactly = 0) { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_BT_AUDIO) }
    }

    @Test
    fun `chooseAudioDevice should cancel pending Bluetooth operation on device change on API 31+`() {
        setupForAPI31Plus(31)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH, id = "4")
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER, id = "1")

        deviceController.chooseAudioDevice(bluetoothDevice)
        deviceController.chooseAudioDevice(speakerDevice)

        verify(exactly = 2) { mockBluetoothAudioRouter.cancelPendingOperation() }
    }

    @Test
    fun `Bluetooth success callback should call setRoute and publish event on API 31+`() {
        setupForAPI31Plus(31)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH, id = "4")

        deviceController.chooseAudioDevice(bluetoothDevice)

        // Simulate success callback from BluetoothAudioRouter
        capturedOnSuccess?.invoke(AudioClient.SPK_STREAM_ROUTE_BT_AUDIO, MediaDeviceType.AUDIO_BLUETOOTH)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_BT_AUDIO) }
        verify {
            eventAnalyticsController.publishEvent(
                EventName.audioInputSelected,
                mutableMapOf(EventAttributeName.audioDeviceType to MediaDeviceType.AUDIO_BLUETOOTH.toString()),
                false
            )
        }
    }

    @Test
    fun `Bluetooth failure callback should notify observers on API 31+`() {
        setupForAPI31Plus(31)
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH, id = "4")

        deviceController.chooseAudioDevice(bluetoothDevice)

        // Simulate failure callback from BluetoothAudioRouter
        capturedOnFailure?.invoke()

        // Should clear communication device on API 31+
        verify { audioManager.clearCommunicationDevice() }
        // Should fall back to speaker (no active non-BT recording device mocked)
        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
        verify { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }

    // ==================== Bluetooth Routing Delegation Tests - API < 31 ====================

    @Test
    fun `chooseAudioDevice should delegate to BluetoothAudioRouter for Bluetooth on API below 31`() {
        setupForAPI30AndBelow(30)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH)

        deviceController.chooseAudioDevice(bluetoothDevice)

        verify { mockBluetoothAudioRouter.routeToBluetoothDevice(bluetoothDevice, AudioClient.SPK_STREAM_ROUTE_BT_AUDIO, any(), any()) }
    }

    @Test
    fun `chooseAudioDevice should immediately call setRoute for speaker on API below 31`() {
        setupForAPI30AndBelow(30)
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER)

        deviceController.chooseAudioDevice(speakerDevice)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
    }

    @Test
    fun `chooseAudioDevice should cancel pending Bluetooth operation on device change on API below 31`() {
        setupForAPI30AndBelow(30)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH)
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER)

        deviceController.chooseAudioDevice(bluetoothDevice)
        deviceController.chooseAudioDevice(speakerDevice)

        verify(exactly = 2) { mockBluetoothAudioRouter.cancelPendingOperation() }
    }

    @Test
    fun `Bluetooth success callback should call setRoute and publish event on API below 31`() {
        setupForAPI30AndBelow(30)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH)

        deviceController.chooseAudioDevice(bluetoothDevice)

        // Simulate success callback from BluetoothAudioRouter
        capturedOnSuccess?.invoke(AudioClient.SPK_STREAM_ROUTE_BT_AUDIO, MediaDeviceType.AUDIO_BLUETOOTH)

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_BT_AUDIO) }
        verify {
            eventAnalyticsController.publishEvent(
                EventName.audioInputSelected,
                mutableMapOf(EventAttributeName.audioDeviceType to MediaDeviceType.AUDIO_BLUETOOTH.toString()),
                false
            )
        }
    }

    @Test
    fun `Bluetooth failure callback should notify observers on API below 31`() {
        setupForAPI30AndBelow(30)
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        val bluetoothDevice = MediaDevice("Bluetooth Headset", MediaDeviceType.AUDIO_BLUETOOTH)

        deviceController.chooseAudioDevice(bluetoothDevice)

        // Simulate failure callback from BluetoothAudioRouter
        capturedOnFailure?.invoke()

        // Should NOT call clearCommunicationDevice on API < 31
        verify(exactly = 0) { audioManager.clearCommunicationDevice() }
        // Should fall back to speaker (no active non-BT recording device mocked)
        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
        verify { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }

    // ==================== Cross-API Tests ====================

    @Test
    fun `listAudioDevices should include device IDs for API 23+`() {
        setupForAPI31Plus(23)

        val devices = deviceController.listAudioDevices()

        devices.forEach { device ->
            assert(device.id != null) { "Device ${device.label} should have an ID" }
        }
    }

    @Test
    fun `chooseAudioDevice should not proceed when AudioClient is not STARTED on API 31+`() {
        setupForAPI31Plus(31)
        DefaultAudioClientController.audioClientState = AudioClientState.STOPPED
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER, id = "1")

        deviceController.chooseAudioDevice(speakerDevice)

        verify(exactly = 0) { audioClientController.setRoute(any()) }
    }

    @Test
    fun `chooseAudioDevice should call publishEvent when audio device is selected on API 31+`() {
        setupForAPI31Plus(31)
        val speakerDevice = MediaDevice("Speaker", MediaDeviceType.AUDIO_BUILTIN_SPEAKER, id = "1")

        deviceController.chooseAudioDevice(speakerDevice)

        verify {
            eventAnalyticsController.publishEvent(
                EventName.audioInputSelected,
                mutableMapOf(EventAttributeName.audioDeviceType to MediaDeviceType.AUDIO_BUILTIN_SPEAKER.toString()),
                false
            )
        }
    }
}
