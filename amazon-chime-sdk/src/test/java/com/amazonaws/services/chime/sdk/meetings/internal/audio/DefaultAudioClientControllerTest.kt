/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.TestConstant
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingStatsCollector
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioDeviceCapabilities
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioRecordingPresetOverride
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioStreamType
import com.amazonaws.services.chime.sdk.meetings.ingestion.VoiceFocusError
import com.amazonaws.services.chime.sdk.meetings.internal.utils.AppInfoUtil
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.MediaError
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AppInfo
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.audio.audioclient.AudioClient.AudioDeviceCapabilitiesInternal
import com.xodee.client.audio.audioclient.AudioClient.AudioModeInternal
import com.xodee.client.audio.audioclient.AudioClient.AudioRecordingPreset
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultAudioClientControllerTest {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockAudioClient: AudioClient

    @MockK
    private lateinit var mockEventAnlyticsStateController: MeetingStatsCollector

    @MockK
    private lateinit var mockEventAnalyticsController: EventAnalyticsController

    @MockK
    private lateinit var mockAudioClientObserver: AudioClientObserver

    private lateinit var audioManager: AudioManager

    private lateinit var audioClientController: DefaultAudioClientController

    private val testDispatcher = TestCoroutineDispatcher()

    private val testAudioClientSuccessCode = 0
    private val testAudioClientFailureCode = 1
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
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        MockKAnnotations.init(this, relaxUnitFun = true)

        setupAudioManager()

        every { mockAudioClientObserver.currentAudioStatus } returns MeetingSessionStatusCode.OK

        audioClientController = DefaultAudioClientController(
            context,
            mockLogger,
            mockAudioClientObserver,
            mockAudioClient,
            mockEventAnlyticsStateController,
            mockEventAnalyticsController
        )
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun setupAudioManager() {
        audioManager = mockkClass(AudioManager::class)
        every { audioManager.mode } returns AudioManager.MODE_NORMAL
        every { audioManager.isSpeakerphoneOn } returns false
        every { audioManager.setBluetoothScoOn(any()) } just runs
        every { audioManager.stopBluetoothSco() } just runs
        every { audioManager.setMode(any()) } just runs
        every { audioManager.setSpeakerphoneOn(any()) } just runs
        every { context.getSystemService(any()) } returns audioManager
    }

    private fun setupRouteTests(audioClientStatusCode: Int) {
        every { mockAudioClient.route } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { mockAudioClient.setRoute(any()) } returns audioClientStatusCode
    }

    private fun setupStartTests() {
        every { mockAudioClient.sendMessage(any(), any()) } returns testAudioClientSuccessCode
        every {
            mockAudioClient.startSession(any())
        } returns testAudioClientSuccessCode
        every { mockAudioClient.setVoiceFocusNoiseSuppression(any()) } returns AudioClient.AUDIO_CLIENT_OK
        every { mockAudioClient.getVoiceFocusNoiseSuppression() } returns true

        mockkStatic(AudioTrack::class, AudioRecord::class)
        every { AudioTrack.getNativeOutputSampleRate(any()) } returns testSampleBuffer
        every { AudioTrack.getMinBufferSize(any(), any(), any()) } returns testSampleBuffer
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns testSampleBuffer
        DefaultAudioClientController.audioClientState = AudioClientState.INITIALIZED

        val testAppInfo = AppInfo(
            "name",
            "versionCode",
            "make",
            "model",
            "version",
            "amazon-chime-sdk",
            "sdkVersion",
            "-07:00"
        )

        mockkObject(AppInfoUtil)
        every { AppInfoUtil.initializeAudioClientAppInfo(any()) } returns testAppInfo
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

        val attributes = mutableMapOf<EventAttributeName, Any>(
            EventAttributeName.audioInputErrorMessage to MediaError.FailedToSetRoute
        )
        verify { mockEventAnalyticsController.publishEvent(EventName.audioInputFailed, attributes) }
    }

    @Test
    fun `setMute should call AudioClient setMicMute`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientSuccessCode

        audioClientController.setMute(testMicMute)

        verify { mockAudioClient.setMicMute(any()) }

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `setMute should return true upon success`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientSuccessCode

        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertTrue(testOutput)

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `setMute should return false upon failure`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientFailureCode

        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertFalse(testOutput)

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `setMute should return true when audio client state is started`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.STARTED
        every { mockAudioClient.setMicMute(any()) } returns testAudioClientSuccessCode

        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertTrue(testOutput)

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `setMute should return false when audio client state is initialized`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.INITIALIZED
        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertFalse(testOutput)

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `setMute should return false when audio client state is stopped`() {
        val state = DefaultAudioClientController.audioClientState
        DefaultAudioClientController.audioClientState = AudioClientState.STOPPED
        val testOutput: Boolean = audioClientController.setMute(testMicMute)

        assertFalse(testOutput)

        DefaultAudioClientController.audioClientState = state
    }

    @Test
    fun `start with Stereo 48KHz should call AudioClient startSession with Stereo 48KHz and mute mic and speaker as false`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertFalse(it.micMute)
                assertFalse(it.spkMute)
                assertEquals(AudioModeInternal.STEREO_48K, it.audioMode)
            })
        }
    }

    @Test
    fun `start with Mono 16KHz should call AudioClient startSession with Mono 16KHz and mute mic and speaker as false`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertFalse(it.micMute)
                assertFalse(it.spkMute)
                assertEquals(AudioModeInternal.MONO_16K, it.audioMode)
            })
        }
    }

    @Test
    fun `start with Mono 48KHz should call AudioClient startSession with Mono 48KHz and mute mic and speaker as false`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertFalse(it.micMute)
                assertFalse(it.spkMute)
                assertEquals(AudioModeInternal.MONO_48K, it.audioMode)
            })
        }
    }

    @Test
    fun `start with audio device capabilities none should call AudioClient startSession with capability none`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.None,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioDeviceCapabilitiesInternal.NONE, it.audioDeviceCapabilities)
            })
        }
    }

    @Test
    fun `start with audio device capabilities output only should call AudioClient startSession with capability output only`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.OutputOnly,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioDeviceCapabilitiesInternal.OUTPUT_ONLY, it.audioDeviceCapabilities)
            })
        }
    }

    @Test
    fun `start with audio device capabilities input and output should call AudioClient startSession with capability input and output`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioDeviceCapabilitiesInternal.INPUT_AND_OUTPUT, it.audioDeviceCapabilities)
            })
        }
    }

    @Test
    fun `start() with voice call stream should call startSession(AudioClientSessionConfig) with AudioStreamType VOICE_CALL`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Mono48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioClient.AudioStreamType.VOICE_CALL, it.audioStreamType)
            })
        }
    }

    @Test
    fun `start() with music stream should call startSession(AudioClientSessionConfig) with AudioStreamType MUSIC`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Mono48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.Music,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioClient.AudioStreamType.MUSIC, it.audioStreamType)
            })
        }
    }

    @Test
    fun `start should call AudioManger setMode`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Stereo48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
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
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify(exactly = 1) { mockAudioClientObserver.notifyAudioClientObserver(any()) }
    }

    @Test
    fun `start should notify EventAnalyticsController about meeting start requested event`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(EventName.meetingStartRequested, any()) }
    }

    @Test
    fun `start should notify EventAnalyticsController about meeting start failed event`() {
        setupStartTests()
        every {
            mockAudioClient.startSession(any())
        } returns testAudioClientFailureCode

        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        verify(exactly = 1) { mockEventAnalyticsController
            .publishEvent(EventName.meetingStartFailed,
                mutableMapOf(EventAttributeName.meetingStatus to MeetingSessionStatusCode.OK)) }
    }

    @Test
    fun `stop should not call AudioClient stopSession when audio client status is not started`() {
        setupStartTests()
        every { mockAudioClient.stopSession() } returns testAudioClientSuccessCode

        audioClientController.stop()

        verify(exactly = 0) { mockAudioClient.stopSession() }
    }

    @Test
    fun `stop should notify EventAnalyticsController about meeting end event`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )
        every { mockAudioClient.stopSession() } returns testAudioClientSuccessCode

        audioClientController.stop()

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockEventAnalyticsController.publishEvent(EventName.meetingEnded, any()) }
    }

    @Test
    fun `stop should call AudioClient stopSession when audio client status is started`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )
        every { mockAudioClient.stopSession() } returns testAudioClientSuccessCode

        audioClientController.stop()

        verify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockAudioClient.stopSession() }
        verify { audioManager.setBluetoothScoOn(false) }
        verify { audioManager.stopBluetoothSco() }
        verify { audioManager.setMode(AudioManager.MODE_NORMAL) }
        verify { audioManager.setSpeakerphoneOn(false) }
    }

    @Test
    fun `setVoiceEnabled should call AudioClient setVoiceFocusNoiseSuppression when audio client status is started`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        val enableOutput: Boolean = audioClientController.setVoiceFocusEnabled(true)
        verify(exactly = 1) { mockAudioClient.setVoiceFocusNoiseSuppression(true) }
        assertTrue(enableOutput)

        val disableOutput: Boolean = audioClientController.setVoiceFocusEnabled(false)
        verify(exactly = 1) { mockAudioClient.setVoiceFocusNoiseSuppression(false) }
        assertTrue(disableOutput)

        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(EventName.voiceFocusEnabled, mutableMapOf(), false) }
    }

    @Test
    fun `setVoiceEnabled should not call AudioClient setVoiceFocusNoiseSuppression when audio client status is not started`() {
        setupStartTests()

        val enableOutput: Boolean = audioClientController.setVoiceFocusEnabled(true)
        verify(exactly = 0) { mockAudioClient.setVoiceFocusNoiseSuppression(any()) }
        assertFalse(enableOutput)

        val disableOutput: Boolean = audioClientController.setVoiceFocusEnabled(false)
        verify(exactly = 0) { mockAudioClient.setVoiceFocusNoiseSuppression(any()) }
        assertFalse(disableOutput)

        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(EventName.voiceFocusEnableFailed, mutableMapOf(
            EventAttributeName.voiceFocusErrorMessage to VoiceFocusError.audioClientNotStarted
        ), false) }
    }

    @Test
    fun `setVoiceEnabled should publish event when audioClient setVoiceFocusNoiseSuppression(enabled) returns error`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        // Mock setVoiceFocusNoiseSuppression to return error code
        every { mockAudioClient.setVoiceFocusNoiseSuppression(true) } returns testAudioClientFailureCode
        every { mockAudioClient.setVoiceFocusNoiseSuppression(false) } returns testAudioClientFailureCode

        // Test enabling VoiceFocus with error
        val enableResult = audioClientController.setVoiceFocusEnabled(true)
        assertFalse(enableResult)
        verify(exactly = 1) { mockAudioClient.setVoiceFocusNoiseSuppression(true) }
        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(
            EventName.voiceFocusEnableFailed,
            mutableMapOf(EventAttributeName.voiceFocusErrorMessage to VoiceFocusError.fromXalError(testAudioClientFailureCode)),
            false
        ) }

        // Test disabling VoiceFocus with error
        val disableResult = audioClientController.setVoiceFocusEnabled(false)
        assertFalse(disableResult)
        verify(exactly = 1) { mockAudioClient.setVoiceFocusNoiseSuppression(false) }
        verify(exactly = 1) { mockEventAnalyticsController.publishEvent(
            EventName.voiceFocusDisableFailed,
            mutableMapOf(EventAttributeName.voiceFocusErrorMessage to VoiceFocusError.fromXalError(testAudioClientFailureCode)),
            false
        ) }
    }

    @Test
    fun `isVoiceEnabled should call AudioClient getVoiceFocusNoiseSuppression when audio client status is started`() {
        setupStartTests()
        audioClientController.start(
            testAudioFallbackUrl,
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Stereo48K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.None,
            true,
            reconnectTimeoutMs = 180000
        )

        audioClientController.isVoiceFocusEnabled()
        verify(exactly = 1) { mockAudioClient.getVoiceFocusNoiseSuppression() }
    }

    @Test
    fun `isVoiceEnabled should not call AudioClient getVoiceFocusNoiseSuppression when audio client status is not started`() {
        setupStartTests()

        audioClientController.isVoiceFocusEnabled()
        verify(exactly = 0) { mockAudioClient.getVoiceFocusNoiseSuppression() }
    }

    @Test
    fun `start with default recording preset when no override provided`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioRecordingPreset.VOICE_COMMUNICATION, it.audioRecordingPreset)
            })
        }
    }

    @Test
    fun `start with recording preset of GENERIC if provided by builder`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.Generic,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioRecordingPreset.GENERIC, it.audioRecordingPreset)
            })
        }
    }

    @Test
    fun `start with recording preset of CAMCORDER if provided by builder`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.Camcorder,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioRecordingPreset.CAMCORDER, it.audioRecordingPreset)
            })
        }
    }

    @Test
    fun `start with recording preset of VOICE_RECOGNITION if provided by builder`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.VoiceRecognition,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioRecordingPreset.VOICE_RECOGNITION, it.audioRecordingPreset)
            })
        }
    }

    @Test
    fun `start with recording preset of VOICE_COMMUNICATION if provided by builder`() {
        setupStartTests()

        audioClientController.start(
                testAudioFallbackUrl,
                testAudioHostUrl,
                testMeetingId,
                testAttendeeId,
                testJoinToken,
                AudioMode.Mono16K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.VoiceCommunication,
                true,
                reconnectTimeoutMs = 180000
        )

        verify {
            mockAudioClient.startSession(withArg {
                assertEquals(AudioRecordingPreset.VOICE_COMMUNICATION, it.audioRecordingPreset)
            })
        }
    }

    @Test(expected = Exception::class)
    fun `start should throw exception if audioHostUrl is blank`() {
        setupStartTests()

        audioClientController.start(
            testAudioFallbackUrl,
            "",
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Mono16K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.VoiceCommunication,
            true,
            reconnectTimeoutMs = 180000
        )
    }

    @Test(expected = Exception::class)
    fun `start should throw exception if audioFallbackUrl is blank`() {
        setupStartTests()

        audioClientController.start(
            "",
            testAudioHostUrl,
            testMeetingId,
            testAttendeeId,
            testJoinToken,
            AudioMode.Mono16K,
            AudioDeviceCapabilities.InputAndOutput,
            AudioStreamType.VoiceCall,
            AudioRecordingPresetOverride.VoiceCommunication,
            true,
            reconnectTimeoutMs = 180000
        )
    }
}
