/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultVideoClientStateControllerTest {
    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockVideoClientLifecycleHandler: VideoClientLifecycleHandler

    private lateinit var testVideoClientStateController: DefaultVideoClientStateController

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testVideoClientStateController =
            DefaultVideoClientStateController(
                mockLogger
            )
        testVideoClientStateController.bindLifecycleHandler(mockVideoClientLifecycleHandler)
    }

    @Test
    fun `start should initialize and start video client when video client state is uninitialized`() {
        testVideoClientStateController.start()

        verify { mockVideoClientLifecycleHandler.initializeVideoClient() }
        verify { mockVideoClientLifecycleHandler.startVideoClient() }
    }

    @Test
    fun `start should only start video client when video client state is initialized`() {
        testVideoClientStateController.updateState(VideoClientState.INITIALIZED)

        testVideoClientStateController.start()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.initializeVideoClient() }
        verify { mockVideoClientLifecycleHandler.startVideoClient() }
    }

    @Test
    fun `start should do nothing when video client state is started`() {
        testVideoClientStateController.updateState(VideoClientState.STARTED)

        testVideoClientStateController.start()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.initializeVideoClient() }
        verify(exactly = 0) { mockVideoClientLifecycleHandler.startVideoClient() }
    }

    @Test
    fun `start should only start video client when video client state is stopped`() {
        testVideoClientStateController.updateState(VideoClientState.STOPPED)

        testVideoClientStateController.start()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.initializeVideoClient() }
        verify { mockVideoClientLifecycleHandler.startVideoClient() }
    }

    @Test
    fun `stop should do nothing when video client state is uninitialized`() {
        testVideoClientStateController.stop()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.stopVideoClient() }
        verify(exactly = 0) { mockVideoClientLifecycleHandler.destroyVideoClient() }
    }

    @Test
    fun `stop should only destroy video client when video client state is initialized`() {
        testVideoClientStateController.updateState(VideoClientState.INITIALIZED)

        testVideoClientStateController.stop()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.stopVideoClient() }
        verify { mockVideoClientLifecycleHandler.destroyVideoClient() }
    }

    @Test
    fun `stop should stop and destroy video client when video client state is started`() {
        testVideoClientStateController.updateState(VideoClientState.STARTED)

        testVideoClientStateController.stop()

        verify { mockVideoClientLifecycleHandler.stopVideoClient() }
        verify { mockVideoClientLifecycleHandler.destroyVideoClient() }
    }

    @Test
    fun `stop should only destroy video client when video client state is stopped`() {
        testVideoClientStateController.updateState(VideoClientState.STOPPED)

        testVideoClientStateController.stop()

        verify(exactly = 0) { mockVideoClientLifecycleHandler.stopVideoClient() }
        verify { mockVideoClientLifecycleHandler.destroyVideoClient() }
    }

    @Test
    fun `canAct should return false when video client state is less than minimum required state`() {
        val testOutput: Boolean =
            testVideoClientStateController.canAct(VideoClientState.INITIALIZED)

        assertFalse(testOutput)
    }

    @Test
    fun `canAct should return true when video client state is equal to minimum required state`() {
        testVideoClientStateController.updateState(VideoClientState.INITIALIZED)

        val testOutput: Boolean =
            testVideoClientStateController.canAct(VideoClientState.INITIALIZED)

        assertTrue(testOutput)
    }

    @Test
    fun `canAct should return true when video client state is greater than minimum required state`() {
        testVideoClientStateController.updateState(VideoClientState.STARTED)

        val testOutput: Boolean =
            testVideoClientStateController.canAct(VideoClientState.INITIALIZED)

        assertTrue(testOutput)
    }
}
