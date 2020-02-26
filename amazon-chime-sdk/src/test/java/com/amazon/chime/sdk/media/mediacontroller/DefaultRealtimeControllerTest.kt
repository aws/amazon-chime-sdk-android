/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.media.clientcontroller.AudioClientObserver
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
    fun `realtimeAddObserver should call audioClientController subscribeToRealTimeEvents with given observer`() {
        realtimeController.realtimeAddObserver(mockObserver)
        verify { audioClientObserver.subscribeToRealTimeEvents(mockObserver) }
    }

    @Test
    fun `realtimeRemoveObserver should call audioClientController unsubscribeFromRealTimeEvents with given observer`() {
        realtimeController.realtimeRemoveObserver(mockObserver)
        verify { audioClientObserver.unsubscribeFromRealTimeEvents(mockObserver) }
    }
}
