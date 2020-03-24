/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.media.AudioDeviceInfo
import android.media.AudioRecord
import android.media.AudioTrack
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultAudioClientControllerTest {
    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockAudioClient: AudioClient

    @MockK
    private lateinit var mockAudioClientObserver: AudioClientObserver

    @InjectMockKs
    private lateinit var audioClientController: DefaultAudioClientController

    private val testDispatcher = TestCoroutineDispatcher()

    private val testAudioClientSuccessCode = 0
    private val testAudioClientFailureCode = -1
    private val testNewRoute = 5
    private val testMicMute = true
    private val testAudioFallbackUrl = "audioFallbackUrl"
    private val testAudioHostUrl = "https://audiohost.com:500"
    private val testMeetingId = "meetingId"
    private val testAttendeeId = "aliceId"
    private val testJoinToken = "joinToken"
    private val testSampleBuffer = 32 // Used for sample rate and buffer size

    @Before
    fun setup() {
        // It appears that mocking Log.d needs to happen before MockKAnnotations.init, or else
        // test will complain that Log.d is not mocked
        mockkStatic(System::class, Log::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs

        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun setupRouteTests(audioClientStatusCode: Int) {
        every { mockAudioClient.route } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { mockAudioClient.setRoute(any()) } returns audioClientStatusCode
    }

    private fun setupStartTests() {
        every { mockAudioClient.sendMessage(any(), any()) } returns testAudioClientSuccessCode
        every {
            mockAudioClient.startSession(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns testAudioClientSuccessCode

        mockkStatic(AudioTrack::class, AudioRecord::class)
        every { AudioTrack.getNativeOutputSampleRate(any()) } returns testSampleBuffer
        every { AudioTrack.getMinBufferSize(any(), any(), any()) } returns testSampleBuffer
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns testSampleBuffer
    }

    @Test
    fun `getRoute should call AudioClient getRoute`() {
        setupRouteTests(testAudioClientSuccessCode)

        audioClientController.getRoute()

        verify { audioClientController.getRoute() }
    }

    @Test
    fun `setRoute should call AudioClient setRoute new route is different from current route`() {
        setupRouteTests(testAudioClientSuccessCode)

        audioClientController.setRoute(testNewRoute)

        verify(exactly = 1) { mockAudioClient.setRoute(any()) }
    }

    @Test
    fun `setRoute should NOT call AudioClient setRoute when new route is same as current route`() {
        every { mockAudioClient.route } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

        audioClientController.setRoute(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

        verify(exactly = 0) { mockAudioClient.route = any() }
    }

    @Test
    fun `setRoute should return true when AudioClient setRoute returns success code`() {
        setupRouteTests(testAudioClientSuccessCode)

        val testOutput: Boolean = audioClientController.setRoute(testNewRoute)

        assertTrue(testOutput)
    }

    @Test
    fun `setRoute should return false when AudioClient setRoute does NOT return success code`() {
        setupRouteTests(testAudioClientFailureCode)

        val testOutput: Boolean = audioClientController.setRoute(testNewRoute)

        assertFalse(testOutput)
    }

    @Test
    fun `setMute should call AudioClient setMicMute`() {
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientSuccessCode

        audioClientController.setMute(testMicMute)

        verify { mockAudioClient.setMicMute(any()) }
    }

    @Test
    fun `setMute should return true upon success`() {
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientSuccessCode

        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertTrue(testOutput)
    }

    @Test
    fun `setMute should return false upon failure`() {
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientFailureCode

        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertFalse(testOutput)
    }

    @Test
    fun `start should call AudioClient startSession`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken
        )

        verify {
            mockAudioClient.startSession(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `start should notify audioClientObserver about audio client connection events`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken
        )

        verify(exactly = 2) { mockAudioClientObserver.notifyAudioClientObserver(any()) }
    }

    @Test
    fun `stop should call AudioClient stopSession`() {
        every { mockAudioClient.stopSession() } returns testAudioClientSuccessCode

        audioClientController.stop()

        verify { mockAudioClient.stopSession() }
    }
}
