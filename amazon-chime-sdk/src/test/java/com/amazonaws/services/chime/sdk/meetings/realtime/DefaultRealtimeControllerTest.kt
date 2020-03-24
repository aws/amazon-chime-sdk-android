/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultRealtimeControllerTest {
    @MockK
    private lateinit var mockObserver: RealtimeObserver

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

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
        realtimeController.addRealtimeObserver(mockObserver)
        verify { audioClientObserver.subscribeToRealTimeEvents(mockObserver) }
    }

    @Test
    fun `removeRealtimeObserver should call audioClientController unsubscribeFromRealTimeEvents with given observer`() {
        realtimeController.removeRealtimeObserver(mockObserver)
        verify { audioClientObserver.unsubscribeFromRealTimeEvents(mockObserver) }
    }
}
