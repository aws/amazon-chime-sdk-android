package com.amazon.chime.sdk.media.clientcontroller

import android.content.Context
import android.media.AudioDeviceInfo
import android.util.Log
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultAudioClientControllerTest {
    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockAudioClient: AudioClient

    @MockK
    private lateinit var mockAudioClientObserver: AudioClientObserver

    @InjectMockKs
    private lateinit var audioClientController: DefaultAudioClientController

    private val testAudioClientSuccessCode = 0
    private val testAudioClientFailureCode = -1
    private val testNewRoute = 5
    private val testMicMute = true

    @Before
    fun setup() {
        // It appears that mocking Log.d needs to happen before MockKAnnotations.init, or else
        // test will complain that Log.d is not mocked
        mockkStatic(System::class, Log::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs

        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    private fun setupRouteTests(audioClientStatusCode: Int) {
        every { mockAudioClient.route } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { mockAudioClient.setRoute(any()) } returns audioClientStatusCode
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
}
