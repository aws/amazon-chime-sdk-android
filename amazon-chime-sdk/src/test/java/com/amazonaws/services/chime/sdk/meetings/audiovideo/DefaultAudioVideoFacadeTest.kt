/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import android.content.Context
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerDetectorFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.DeviceController
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeControllerFacade
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultAudioVideoFacadeTest {
    private val devices = emptyList<MediaDevice>()

    private val mediaDevice = MediaDevice(
        "label",
        MediaDeviceType.OTHER
    )

    private val messageTopic = "topic"
    private val messageData = "data"
    private val messageLifetimeMs = 3000

    @MockK
    private lateinit var mockAudioVideoObserver: AudioVideoObserver

    @MockK
    private lateinit var mockRealtimeObserver: RealtimeObserver

    @MockK
    private lateinit var mockEventAnalyticsObserver: EventAnalyticsObserver

    @MockK
    private lateinit var mockDataMessageObserver: DataMessageObserver

    @MockK
    private lateinit var mockDeviceChangeObserver: DeviceChangeObserver

    @MockK
    private lateinit var mockMetricsObserver: MetricsObserver

    @MockK
    private lateinit var mockContentShareObserver: ContentShareObserver

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioVideoController: AudioVideoControllerFacade

    @MockK
    private lateinit var eventAnalyticsController: EventAnalyticsController

    @MockK
    private lateinit var realtimeController: RealtimeControllerFacade

    @MockK
    private lateinit var deviceController: DeviceController

    @MockK
    private lateinit var videoTileController: VideoTileController

    @MockK
    private lateinit var activeSpeakerDetector: ActiveSpeakerDetectorFacade

    @MockK
    private lateinit var contentShareController: ContentShareController

    @InjectMockKs
    private lateinit var audioVideoFacade: DefaultAudioVideoFacade

    @Before
    fun setup() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test(expected = SecurityException::class)
    fun `start should throw exception when the required permissions are not granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns 1
        audioVideoFacade.start()
    }

    @Test
    fun `addEventAnalyticsObserver should call EvenController addEventAnalyticsObserver with given observer`() {
        audioVideoFacade.addEventAnalyticsObserver(mockEventAnalyticsObserver)

        verify { eventAnalyticsController.addEventAnalyticsObserver(mockEventAnalyticsObserver) }
    }

    @Test
    fun `addAudioVideoObserver should call audioVideoController addObserver with given observer`() {
        audioVideoFacade.addAudioVideoObserver(mockAudioVideoObserver)
        verify { audioVideoController.addAudioVideoObserver(mockAudioVideoObserver) }
    }

    @Test
    fun `removeAudioVideoObserver should call audioVideoController removeObserve with given observer`() {
        audioVideoFacade.removeAudioVideoObserver(mockAudioVideoObserver)
        verify { audioVideoController.removeAudioVideoObserver(mockAudioVideoObserver) }
    }

    @Test
    fun `addMetricsObserver should call audioVideoController addMetricsObserver with given observer`() {
        audioVideoFacade.addMetricsObserver(mockMetricsObserver)
        verify { audioVideoController.addMetricsObserver(mockMetricsObserver) }
    }

    @Test
    fun `removeEventAnalyticsObserver should call EvenAnalyticsController removeEventAnalyticsObserver with given observer`() {
        audioVideoFacade.removeEventAnalyticsObserver(mockEventAnalyticsObserver)

        verify { eventAnalyticsController.removeEventAnalyticsObserver(mockEventAnalyticsObserver) }
    }

    @Test
    fun `removeMetricsObserver should call audioVideoController removeMetricsObserver with given observer`() {
        audioVideoFacade.removeMetricsObserver(mockMetricsObserver)
        verify { audioVideoController.removeMetricsObserver(mockMetricsObserver) }
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
    fun `addRealtimeObserver should call realtimeController addRealtimeObserver with given observer`() {
        audioVideoFacade.addRealtimeObserver(mockRealtimeObserver)
        verify { realtimeController.addRealtimeObserver(mockRealtimeObserver) }
    }

    @Test
    fun `removeRealtimeObserver should call realtimeController removeRealtimeObserver with given observer`() {
        audioVideoFacade.removeRealtimeObserver(mockRealtimeObserver)
        verify { realtimeController.removeRealtimeObserver(mockRealtimeObserver) }
    }

    @Test
    fun `realtimeSendDataMessage should call realtimeController realtimeSendDataMessage with given data`() {
        audioVideoFacade.realtimeSendDataMessage(messageTopic, messageData, messageLifetimeMs)
        verify { realtimeController.realtimeSendDataMessage(messageTopic, messageData, messageLifetimeMs) }
    }

    @Test
    fun `realtimeSendDataMessage should call realtimeController realtimeSendDataMessage with 0 lifetime ms as default`() {
        audioVideoFacade.realtimeSendDataMessage(messageTopic, messageData)
        verify { realtimeController.realtimeSendDataMessage(messageTopic, messageData, 0) }
    }

    @Test
    fun `addRealtimeDataMessageObserver should call realtimeController addRealtimeDataMessageObserver with given observer`() {
        audioVideoFacade.addRealtimeDataMessageObserver(messageTopic, mockDataMessageObserver)
        verify { realtimeController.addRealtimeDataMessageObserver(messageTopic, mockDataMessageObserver) }
    }

    @Test
    fun `removeRealtimeDataMessageObserverFromTopic should call realtimeController removeRealtimeDataMessageObserverFromTopic with given topic`() {
        audioVideoFacade.removeRealtimeDataMessageObserverFromTopic(messageTopic)
        verify { realtimeController.removeRealtimeDataMessageObserverFromTopic(messageTopic) }
    }

    @Test
    fun `realtimeSetVoiceFocusEnabled(true) should call realtimeController realtimeSetVoiceFocusEnabled(true) and return the status`() {
        every { realtimeController.realtimeSetVoiceFocusEnabled(true) } returns true
        assertTrue(audioVideoFacade.realtimeSetVoiceFocusEnabled(true))
        verify { realtimeController.realtimeSetVoiceFocusEnabled(true) }
    }

    @Test
    fun `realtimeIsVoiceFocusEnabled should call realtimeController realtimeIsVoiceFocusEnabled and return the status true`() {
        every { realtimeController.realtimeIsVoiceFocusEnabled() } returns true
        assertTrue(audioVideoFacade.realtimeIsVoiceFocusEnabled())
        verify { realtimeController.realtimeIsVoiceFocusEnabled() }
    }

    @Test
    fun `realtimeSetVoiceFocusEnabled(false) should call realtimeController realtimeSetVoiceFocusEnabled(false) and return the status`() {
        every { realtimeController.realtimeSetVoiceFocusEnabled(false) } returns true
        assertTrue(audioVideoFacade.realtimeSetVoiceFocusEnabled(false))
        verify { realtimeController.realtimeSetVoiceFocusEnabled(false) }
    }

    @Test
    fun `realtimeIsVoiceFocusEnabled should call realtimeController realtimeIsVoiceFocusEnabled and return the status false`() {
        every { realtimeController.realtimeIsVoiceFocusEnabled() } returns false
        assertFalse(audioVideoFacade.realtimeIsVoiceFocusEnabled())
        verify { realtimeController.realtimeIsVoiceFocusEnabled() }
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
        audioVideoFacade.addDeviceChangeObserver(mockDeviceChangeObserver)
        verify { deviceController.addDeviceChangeObserver(mockDeviceChangeObserver) }
    }

    @Test
    fun `removeDeviceChangeObserver should call deviceController removeDeviceChangeObserver with given observer`() {
        audioVideoFacade.removeDeviceChangeObserver(mockDeviceChangeObserver)
        verify { deviceController.removeDeviceChangeObserver(mockDeviceChangeObserver) }
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

    @Test
    fun `startContentShare should call contentShareController startContentShare`() {
        val source = mockkClass(ContentShareSource::class)

        audioVideoFacade.startContentShare(source)

        verify { contentShareController.startContentShare(source) }
    }

    @Test
    fun `stopContentShare should call contentShareController stopContentShare`() {
        audioVideoFacade.stopContentShare()

        verify { contentShareController.stopContentShare() }
    }

    @Test
    fun `addContentShareObserver should call deviceController addContentShareObserver with given observer`() {
        audioVideoFacade.addContentShareObserver(mockContentShareObserver)

        verify { contentShareController.addContentShareObserver(mockContentShareObserver) }
    }

    @Test
    fun `removeContentShareObserver should call deviceController removeContentShareObserver with given observer`() {
        audioVideoFacade.removeContentShareObserver(mockContentShareObserver)

        verify { contentShareController.removeContentShareObserver(mockContentShareObserver) }
    }
}
