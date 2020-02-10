package com.amazon.chime.sdk.media

import android.content.Context
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.media.devicecontroller.DeviceController
import com.amazon.chime.sdk.media.devicecontroller.MediaDevice
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
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
    private val observer = object : AudioVideoObserver, RealtimeObserver {
        override fun onAudioVideoStartConnecting(reconnecting: Boolean) {
        }

        override fun onAudioVideoStart(reconnecting: Boolean) {
        }

        override fun onAudioVideoStop(sessionStatus: MeetingSessionStatus) {
        }

        override fun onAudioReconnectionCancel() {
        }

        override fun onConnectionRecovered() {
        }

        override fun onConnectionBecamePoor() {
        }

        override fun onVolumeChange(attendeeVolumes: Map<String, Int>) {
        }

        override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, Int>) {
        }
    }

    private val devices = emptyList<MediaDevice>()

    private val mediaDevice = MediaDevice("label", 0)

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioVideoController: AudioVideoControllerFacade

    @MockK
    private lateinit var realtimeController: RealtimeControllerFacade

    @MockK
    private lateinit var deviceController: DeviceController

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
    fun `listAudioInputDevices should call devices deviceController listAudioInputDevices and return the list of devices`() {
        every { deviceController.listAudioInputDevices() } returns devices
        assertEquals(devices, audioVideoFacade.listAudioInputDevices())
    }

    @Test
    fun `listAudioOutputDevices should call devices deviceController listAudioOutputDevices and return the list of devices`() {
        every { deviceController.listAudioOutputDevices() } returns devices
        assertEquals(devices, audioVideoFacade.listAudioOutputDevices())
    }

    @Test
    fun `chooseAudioInputDevice should call deviceController chooseAudioInputDevice`() {
        audioVideoFacade.chooseAudioInputDevice(mediaDevice)
        verify { deviceController.chooseAudioInputDevice(mediaDevice) }
    }

    @Test
    fun `chooseAudioOutputDevice should call deviceController chooseAudioOutputDevice`() {
        audioVideoFacade.chooseAudioOutputDevice(mediaDevice)
        verify { deviceController.chooseAudioOutputDevice(mediaDevice) }
    }
}
