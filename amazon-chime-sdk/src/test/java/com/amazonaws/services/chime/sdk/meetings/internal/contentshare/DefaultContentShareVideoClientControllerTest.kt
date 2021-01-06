/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import android.content.Context
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.utils.AppInfoUtil
import com.amazonaws.services.chime.sdk.meetings.internal.video.DefaultVideoClientFactory
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultContentShareVideoClientControllerTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockContentShareVideoClientObserver: DefaultContentShareVideoClientObserver

    @MockK(relaxed = true)
    private lateinit var mockConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockVideoClient: VideoClient

    @MockK
    private lateinit var mockVideoClientFactory: DefaultVideoClientFactory

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    @MockK(relaxed = true)
    private lateinit var mockEglCore: EglCore

    @InjectMockKs
    private lateinit var testContentShareVideoClientController: DefaultContentShareVideoClientController

    @MockK
    private lateinit var mockVideoSource: VideoSource

    @MockK
    private lateinit var mockContentShareObserver: ContentShareObserver

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        mockkStatic(System::class, Log::class, VideoClient::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs
        every { VideoClient.javaInitializeGlobals(any()) } returns true
        mockkObject(AppInfoUtil)
        every { AppInfoUtil.initializeVideoClientAppDetailedInfo(any()) } just runs
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockVideoClientFactory.getVideoClient(mockContentShareVideoClientObserver) } returns mockVideoClient
        every { mockEglCoreFactory.createEglCore() } returns mockEglCore
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `startVideoShare should call VideoClientStateController setExternalVideoSource`() {
        every { mockVideoClient.start(any()) } returns true
        testContentShareVideoClientController.startVideoShare(mockVideoSource)

        verify(exactly = 1) { mockVideoClient.setExternalVideoSource(any(), any()) }
    }

    @Test
    fun `stopVideoShare should call VideoClientStateController stopService`() {
        testContentShareVideoClientController.stopVideoShare()

        verify(exactly = 1) { mockVideoClient.javaStopService() }
    }

    @Test
    fun `startVideoShare should not call VideoClientStateController startService again when is sharing`() {
        every { mockVideoClient.start(any()) } returns true
        testContentShareVideoClientController.startVideoShare(mockVideoSource)

        testContentShareVideoClientController.startVideoShare(mockVideoSource)

        verify(exactly = 1) { mockVideoClient.start(any()) }
    }

    @Test
    fun `startVideoShare should notify subscribed observer of onContentShareStopped event when failed`() {
        every { mockVideoClient.start(any()) } returns false
        testContentShareVideoClientController.subscribeToVideoClientStateChange(mockContentShareObserver)

        testContentShareVideoClientController.startVideoShare(mockVideoSource)

        verify(exactly = 1) { mockContentShareObserver.onContentShareStopped(any()) }
    }

    @Test
    fun `subscribeToVideoClientStateChange should call contentShareVideoClientObserver subscribeToVideoClientStateChange with given observer`() {
        testContentShareVideoClientController.subscribeToVideoClientStateChange(mockContentShareObserver)

        verify(exactly = 1) { mockContentShareVideoClientObserver.subscribeToVideoClientStateChange(mockContentShareObserver) }
    }

    @Test
    fun `unsubscribeFromVideoClientStateChange should call contentShareVideoClientObserver unsubscribeFromVideoClientStateChange with given observer`() {
        testContentShareVideoClientController.unsubscribeFromVideoClientStateChange(mockContentShareObserver)

        verify(exactly = 1) { mockContentShareVideoClientObserver.unsubscribeFromVideoClientStateChange(mockContentShareObserver) }
    }
}
