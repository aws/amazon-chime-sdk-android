/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media

import android.content.Context
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.media.clientcontroller.ObservableMetric
import com.amazon.chime.sdk.media.devicecontroller.DeviceChangeObserver
import com.amazon.chime.sdk.media.devicecontroller.DeviceController
import com.amazon.chime.sdk.media.devicecontroller.MediaDevice
import com.amazon.chime.sdk.media.devicecontroller.MediaDeviceType
import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileController
import com.amazon.chime.sdk.session.MeetingSessionStatus
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultAudioVideoFacadeTest {
    private val observer = object : AudioVideoObserver, RealtimeObserver, DeviceChangeObserver {

        override fun onAudioClientConnecting(reconnecting: Boolean) {
        }

        override fun onAudioClientStart(reconnecting: Boolean) {
        }

        override fun onAudioClientStop(sessionStatus: MeetingSessionStatus) {
        }

        override fun onAudioClientReconnectionCancel() {
        }

        override fun onConnectionRecover() {
        }

        override fun onConnectionBecomePoor() {
        }

        override fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>) {
        }

        override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>) {
        }

        override fun onAttendeesJoin(attendeeIds: Array<String>) {
        }

        override fun onAttendeesLeave(attendeeIds: Array<String>) {
        }

        override fun onAudioDeviceChange(freshAudioDeviceList: List<MediaDevice>) {
        }

        override fun onReceiveMetric(metrics: Map<ObservableMetric, Any>) {
        }

        override fun onVideoClientConnecting() {
        }

        override fun onVideoClientStart() {
        }

        override fun onVideoClientStop(sessionStatus: MeetingSessionStatus) {
        }
    }

    private val devices = emptyList<MediaDevice>()

    private val mediaDevice = MediaDevice("label", MediaDeviceType.OTHER)

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioVideoController: AudioVideoControllerFacade

    @MockK
    private lateinit var realtimeController: RealtimeControllerFacade

    @MockK
    private lateinit var deviceController: DeviceController

    @MockK
    private lateinit var videoTileController: VideoTileController

    @InjectMockKs
    private lateinit var audioVideoFacade: DefaultAudioVideoFacade

    @Before
    fun setup() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `addObserver should call audioVideoController addObserver with given observer`() {
        audioVideoFacade.addObserver(observer)
        verify { audioVideoController.addObserver(observer) }
    }

    @Test
    fun `removeObserver should call audioVideoController removeObserve with given observer`() {
        audioVideoFacade.removeObserver(observer)
        verify { audioVideoController.removeObserver(observer) }
    }

    @Test(expected = SecurityException::class)
    fun `start should throw exception when the required permissions are not granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns 1
        audioVideoFacade.start()
    }

    @Test
    fun `start should call audioVideoController start when the required permissions are granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns 0
        audioVideoFacade.start()
        verify { audioVideoController.start() }
    }

    @Test
    fun `stop should call audioVideoController stop`() {
        audioVideoFacade.stop()
        verify { audioVideoController.stop() }
    }

    @Test
    fun `realtimeLocalMute should call realtimeController realtimeLocalMute and return the status`() {
        every { realtimeController.realtimeLocalMute() } returns true
        assertTrue(audioVideoFacade.realtimeLocalMute())
    }

    @Test
    fun `realtimeLocalUnmute should call realtimeController realtimeLocalUnmute and return the status`() {
        every { realtimeController.realtimeLocalUnmute() } returns true
        assertTrue(audioVideoFacade.realtimeLocalUnmute())
    }

    @Test
    fun `realtimeAddObserver should call realtimeController realtimeAddObserver with given observer`() {
        audioVideoFacade.realtimeAddObserver(observer)
        verify { realtimeController.realtimeAddObserver(observer) }
    }

    @Test
    fun `realtimeRemoveObserver should call realtimeController realtimeRemoveObserver with given observer`() {
        audioVideoFacade.realtimeRemoveObserver(observer)
        verify { realtimeController.realtimeRemoveObserver(observer) }
    }

    @Test
    fun `listAudioDevices should call devices deviceController listAudioDevices and return the list of devices`() {
        every { deviceController.listAudioDevices() } returns devices
        assertEquals(devices, audioVideoFacade.listAudioDevices())
    }

    @Test
    fun `chooseAudioDevice should call deviceController chooseAudioDevice`() {
        audioVideoFacade.chooseAudioDevice(mediaDevice)
        verify { deviceController.chooseAudioDevice(mediaDevice) }
    }

    @Test
    fun `addDeviceChangeObserver should call deviceController addDeviceChangeObserver with given observer`() {
        audioVideoFacade.addDeviceChangeObserver(observer)
        verify { deviceController.addDeviceChangeObserver(observer) }
    }

    @Test
    fun `removeDeviceChangeObserver should call deviceController removeDeviceChangeObserver with given observer`() {
        audioVideoFacade.removeDeviceChangeObserver(observer)
        verify { deviceController.removeDeviceChangeObserver(observer) }
    }

    @Test
    fun `startLocalVideo should call audioVideoController startLocalVideo`() {
        audioVideoFacade.startLocalVideo()
        verify { audioVideoController.startLocalVideo() }
    }

    @Test
    fun `stopLocalVideo should call audioVideoController stopLocalVideo`() {
        audioVideoFacade.stopLocalVideo()
        verify { audioVideoController.stopLocalVideo() }
    }

    @Test
    fun `getActiveCamera should call deviceController getActiveCamera`() {
        every { deviceController.getActiveCamera() } returns mediaDevice

        assertEquals(mediaDevice, audioVideoFacade.getActiveCamera())
        verify { deviceController.getActiveCamera() }
    }

    @Test
    fun `switchCamera should call deviceController switchCamera`() {
        audioVideoFacade.switchCamera()

        verify { deviceController.switchCamera() }
    }
}
