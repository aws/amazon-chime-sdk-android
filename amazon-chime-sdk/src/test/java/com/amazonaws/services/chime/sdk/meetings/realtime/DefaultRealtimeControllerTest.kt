/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultRealtimeControllerTest {
    private val messageTopic = "topic"
    private val messageData = "data"
    private val messageLifetimeMs = 3000

    @MockK
    private lateinit var mockRealtimeObserver: RealtimeObserver

    @MockK
    private lateinit var mockDataMessageObserver: DataMessageObserver

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var videoClientObserver: VideoClientObserver

    @InjectMockKs
    private lateinit var realtimeController: DefaultRealtimeController

    @Before
    fun setup() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `realtimeLocalMute should call audioClientController setMute with true and return the status`() {
        every { audioClientController.setMute(true) } returns true
        assertTrue(realtimeController.realtimeLocalMute())
        verify { audioClientController.setMute(true) }
    }

    @Test
    fun `realtimeLocalUnmute should call audioClientController setMute with false and return the status`() {
        every { audioClientController.setMute(false) } returns true
        assertTrue(realtimeController.realtimeLocalUnmute())
        verify { audioClientController.setMute(false) }
    }

    @Test
    fun `addRealtimeObserver should call audioClientController subscribeToRealTimeEvents with given observer`() {
        realtimeController.addRealtimeObserver(mockRealtimeObserver)
        verify { audioClientObserver.subscribeToRealTimeEvents(mockRealtimeObserver) }
    }

    @Test
    fun `removeRealtimeObserver should call audioClientController unsubscribeFromRealTimeEvents with given observer`() {
        realtimeController.removeRealtimeObserver(mockRealtimeObserver)
        verify { audioClientObserver.unsubscribeFromRealTimeEvents(mockRealtimeObserver) }
    }

    @Test
    fun `realtimeSendDataMessage should call videoClientController sendDataMessage with given data`() {
        realtimeController.realtimeSendDataMessage(messageTopic, messageData, messageLifetimeMs)
        verify { videoClientController.sendDataMessage(messageTopic, messageData, messageLifetimeMs) }
    }

    @Test
    fun `addRealtimeDataMessageObserver should call videoClientObserver subscribeToReceiveDataMessage with given observer`() {
        realtimeController.addRealtimeDataMessageObserver(messageTopic, mockDataMessageObserver)
        verify { videoClientObserver.subscribeToReceiveDataMessage(messageTopic, mockDataMessageObserver) }
    }

    @Test
    fun `removeRealtimeDataMessageObserverFromTopic should call videoClientObserver unsubscribeFromReceiveDataMessage with given topic`() {
        realtimeController.removeRealtimeDataMessageObserverFromTopic(messageTopic)
        verify { videoClientObserver.unsubscribeFromReceiveDataMessage(messageTopic) }
    }

    @Test
    fun `realtimeSetVoiceFocusEnabled(true) should call audioClientController setVoiceFocusEnabled with true and return the status`() {
        every { audioClientController.setVoiceFocusEnabled(true) } returns true
        assertTrue(realtimeController.realtimeSetVoiceFocusEnabled(true))
        verify { audioClientController.setVoiceFocusEnabled(true) }
    }

    @Test
    fun `realtimeIsVoiceFocusEnabled() should call audioClientController IsVoiceFocusEnabled and return the status true`() {
        every { audioClientController.isVoiceFocusEnabled() } returns true
        assertTrue(realtimeController.realtimeIsVoiceFocusEnabled())
        verify { audioClientController.isVoiceFocusEnabled() }
    }

    @Test
    fun `realtimeSetVoiceFocusEnabled(false) should call audioClientController setVoiceFocusEnabled with false and return the status`() {
        every { audioClientController.setVoiceFocusEnabled(false) } returns true
        assertTrue(realtimeController.realtimeSetVoiceFocusEnabled(false))
        verify { audioClientController.setVoiceFocusEnabled(false) }
    }

    @Test
    fun `realtimeIsVoiceFocusEnabled() should call audioClientController IsVoiceFocusEnabled and return the status false`() {
        every { audioClientController.isVoiceFocusEnabled() } returns false
        assertFalse(realtimeController.realtimeIsVoiceFocusEnabled())
        verify { audioClientController.isVoiceFocusEnabled() }
    }
}
