/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import java.security.InvalidParameterException
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

class DefaultVideoClientControllerTest {
    private val messageTopic = "topic"
    private val messageData = "data"
    private val messageLifetimeMs = 3000

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockPackageInfo: PackageInfo

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockVideoClientStateController: VideoClientStateController

    @MockK
    private lateinit var mockVideoClientObserver: VideoClientObserver

    @MockK
    private lateinit var mockConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockVideoClient: VideoClient

    @MockK
    private lateinit var mockVideoClientFactory: DefaultVideoClientFactory

    @InjectMockKs
    private lateinit var testVideoClientController: DefaultVideoClientController

    @Before
    fun setUp() {
        mockkStatic(System::class, Log::class, VideoClient::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockVideoClient.sendDataMessage(any(), any(), any()) } just runs
        every { mockVideoClientFactory.getVideoClient(mockVideoClientObserver) } returns mockVideoClient
    }

    @Test
    fun `start should call VideoClientStateController start`() {
        testVideoClientController.start()

        verify { mockVideoClientStateController.start() }
    }

    @Test
    fun `startLocalVideo should call VideoClientStateController stop`() {
        runBlockingTest {
            testVideoClientController.stopAndDestroy()
        }

        coVerify { mockVideoClientStateController.stop() }
    }

    @Test
    fun `sendDataMessage should not call videoClient when state is not started`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns false

        testVideoClientController.sendDataMessage(messageTopic, messageData, messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test
    fun `sendDataMessage should call videoClient when state is started`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, messageData, messageLifetimeMs)

        verify(exactly = 1) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when topic is invalid`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage("!@#$%", messageData, messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when data is oversize`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, ByteArray(2049), messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when lifetime ms is negative`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, messageData, -1)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }
}
