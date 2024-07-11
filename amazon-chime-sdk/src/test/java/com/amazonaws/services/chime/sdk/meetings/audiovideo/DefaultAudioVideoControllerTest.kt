/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioDeviceCapabilities
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioRecordingPresetOverride
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioStreamType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.LocalVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientObserver
import com.amazonaws.services.chime.sdk.meetings.internal.metric.ClientMetricsCollector
import com.amazonaws.services.chime.sdk.meetings.internal.utils.AppInfoUtil
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingFeatures
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionURLs
import com.amazonaws.services.chime.sdk.meetings.session.defaultUrlRewriter
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

class DefaultAudioVideoControllerTest {
    @MockK
    private lateinit var audioVideo: AudioVideoObserver
    @MockK
    private lateinit var metricsObserver: MetricsObserver

    private val meetingId = "meetingId"
    private val externalMeetingId = "externalMeetingId"
    private val attendeeId = "attendeeId"
    private val externalUserId = "externalUserId"
    private val joinToken = "joinToken"
    private val audioFallbackURL = "audioFallbackURL"
    private val audioHostURL = "audioHostURL"
    private val turnControlURL = "turnControlURL"
    private val signalingURL = "signalingURL"

    private val meetingSessionConfiguration = MeetingSessionConfiguration(
        meetingId,
        externalMeetingId,
        MeetingSessionCredentials(attendeeId, externalUserId, joinToken),
        MeetingSessionURLs(audioFallbackURL, audioHostURL, turnControlURL, signalingURL, ::defaultUrlRewriter)
    )

    private val meetingSessionConfigurationNone = MeetingSessionConfiguration(
        meetingId,
        externalMeetingId,
        MeetingSessionCredentials(attendeeId, externalUserId, joinToken),
        MeetingSessionURLs(audioFallbackURL, audioHostURL, turnControlURL, signalingURL, ::defaultUrlRewriter),
        MeetingFeatures(videoMaxResolution = VideoResolution.Disabled, contentMaxResolution = VideoResolution.Disabled)
    )

    private val meetingSessionConfigurationHigh = MeetingSessionConfiguration(
        meetingId,
        externalMeetingId,
        MeetingSessionCredentials(attendeeId, externalUserId, joinToken),
        MeetingSessionURLs(audioFallbackURL, audioHostURL, turnControlURL, signalingURL, ::defaultUrlRewriter),
        MeetingFeatures(videoMaxResolution = VideoResolution.VideoResolutionFHD, contentMaxResolution = VideoResolution.VideoResolutionUHD)
    )

    private val localVideoConfig = LocalVideoConfiguration()

    @MockK
    private lateinit var audioClientObserver: AudioClientObserver

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var clientMetricsCollector: ClientMetricsCollector

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var videoClientObserver: VideoClientObserver

    @MockK
    private lateinit var mockPrimaryMeetingSessionCredentials: MeetingSessionCredentials

    @MockK
    private lateinit var mockPrimaryMeetingPromotionObserver: PrimaryMeetingPromotionObserver

    @MockK
    private lateinit var mockTimer: Timer

    @MockK
    private lateinit var mockVideoSource: VideoSource

    @MockK
    private lateinit var mockVideoClient: VideoClient

    private lateinit var audioVideoController: DefaultAudioVideoController
    private lateinit var audioVideoControllerNone: DefaultAudioVideoController
    private lateinit var audioVideoControllerHigh: DefaultAudioVideoController

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        mockkStatic(System::class, Log::class, VideoClient::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs
        every { VideoClient.javaInitializeGlobals(any()) } returns true
        mockkObject(AppInfoUtil)
        every { AppInfoUtil.initializeVideoClientAppDetailedInfo(any()) } just runs
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        val logger = ConsoleLogger(LogLevel.INFO)

        audioVideoController =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                clientMetricsCollector,
                meetingSessionConfiguration,
                videoClientController,
                videoClientObserver,
                logger
            )
        audioVideoControllerNone =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                clientMetricsCollector,
                meetingSessionConfigurationNone,
                videoClientController,
                videoClientObserver,
                logger
            )
        audioVideoControllerHigh =
            DefaultAudioVideoController(
                audioClientController,
                audioClientObserver,
                clientMetricsCollector,
                meetingSessionConfigurationHigh,
                videoClientController,
                videoClientObserver,
                logger
            )
    }

    @Test
    fun `start should call audioClientController start with the parameters in configuration`() {
        audioVideoController.start()
        verify {
            audioClientController.start(
                audioFallbackURL,
                audioHostURL,
                meetingId,
                attendeeId,
                joinToken,
                AudioMode.Stereo48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start with mono 16KHz should call audioClientController start with the parameters in configuration and mono 16KHz`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(audioMode = AudioMode.Mono16K)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                    audioFallbackURL,
                    audioHostURL,
                    meetingId,
                    attendeeId,
                    joinToken,
                    AudioMode.Mono16K,
                    AudioDeviceCapabilities.InputAndOutput,
                    AudioStreamType.VoiceCall,
                    AudioRecordingPresetOverride.None,
                    true,
                    reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start with mono 48KHz should call audioClientController start with the parameters in configuration and mono 48KHz`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(audioMode = AudioMode.Mono48K)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                    audioFallbackURL,
                    audioHostURL,
                    meetingId,
                    attendeeId,
                    joinToken,
                    AudioMode.Mono48K,
                    AudioDeviceCapabilities.InputAndOutput,
                    AudioStreamType.VoiceCall,
                    AudioRecordingPresetOverride.None,
                    true,
                    reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start with stereo 48KHz should call audioClientController start with the parameters in configuration and stereo 48KHz`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(audioMode = AudioMode.Stereo48K)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                    audioFallbackURL,
                    audioHostURL,
                    meetingId,
                    attendeeId,
                    joinToken,
                    AudioMode.Stereo48K,
                    AudioDeviceCapabilities.InputAndOutput,
                    AudioStreamType.VoiceCall,
                    AudioRecordingPresetOverride.None,
                    true,
                    reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start with specified audio device capabilities should call audioClientController start with the parameters in configuration and the correct audio device capabilities`() {
        for (capabilities in AudioDeviceCapabilities.values()) {
            val testAudioVideoConfiguration = AudioVideoConfiguration(audioDeviceCapabilities = capabilities)
            audioVideoController.start(testAudioVideoConfiguration)
            verify {
                audioClientController.start(
                    audioFallbackURL,
                    audioHostURL,
                    meetingId,
                    attendeeId,
                    joinToken,
                    AudioMode.Stereo48K,
                    capabilities,
                    AudioStreamType.VoiceCall,
                    AudioRecordingPresetOverride.None,
                    true,
                    reconnectTimeoutMs = 180000
                )
            }
        }
    }

    @Test
    fun `start() with voice call stream should call audioClientController start() with AudioStreamType VoiceCall`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(audioStreamType = AudioStreamType.VoiceCall)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                audioFallbackURL,
                audioHostURL,
                meetingId,
                attendeeId,
                joinToken,
                AudioMode.Stereo48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start() with music stream should call audioClientController start() with AudioStreamType Music`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(audioStreamType = AudioStreamType.Music)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                audioFallbackURL,
                audioHostURL,
                meetingId,
                attendeeId,
                joinToken,
                AudioMode.Stereo48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.Music,
                AudioRecordingPresetOverride.None,
                true,
                reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start with enableAudioRedundancy as false should call audioClientController start with the parameters in configuration and enableAudioRedundancy set to false`() {
        val testAudioVideoConfiguration = AudioVideoConfiguration(enableAudioRedundancy = false)
        audioVideoController.start(testAudioVideoConfiguration)
        verify {
            audioClientController.start(
                audioFallbackURL,
                audioHostURL,
                meetingId,
                attendeeId,
                joinToken,
                AudioMode.Stereo48K,
                AudioDeviceCapabilities.InputAndOutput,
                AudioStreamType.VoiceCall,
                AudioRecordingPresetOverride.None,
                false,
                reconnectTimeoutMs = 180000
            )
        }
    }

    @Test
    fun `start should call videoClientController start with the parameters in configuration`() {
        audioVideoController.start()
        verify {
            videoClientController.start()
        }
    }

    @Test
    fun `stop should call audioClientController stop`() {
        audioVideoController.stop()

        verify { audioClientController.stop() }
    }

    @Test
    fun `stop should call videoClientController stopAndDestroy`() {
        audioVideoController.stop()

        verify { videoClientController.stopAndDestroy() }
    }

    @Test
    fun `addAudioVideoObserver should call audioClientObserver subscribeToAudioClientStateChange with given observer`() {
        audioVideoController.addAudioVideoObserver(audioVideo)

        verify { audioClientObserver.subscribeToAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `removeAudioVideoObserver should call audioClientObserver unsubscribeFromAudioClientStateChange with given observer`() {
        audioVideoController.removeAudioVideoObserver(audioVideo)

        verify { audioClientObserver.unsubscribeFromAudioClientStateChange(audioVideo) }
    }

    @Test
    fun `addAudioVideoObserver should call videoClientObserver subscribeToVideoClientStateChange with given observer`() {
        audioVideoController.addAudioVideoObserver(audioVideo)

        verify { videoClientObserver.subscribeToVideoClientStateChange(audioVideo) }
    }

    @Test
    fun `removeAudioVideoObserver should call videoClientObserver unsubscribeFromAVideoClientStateChange with given observer`() {
        audioVideoController.removeAudioVideoObserver(audioVideo)

        verify { videoClientObserver.unsubscribeFromVideoClientStateChange(audioVideo) }
    }

    @Test
    fun `addMetricsObserver should call clientMetricsCollector addObserver with given observer`() {
        audioVideoController.addMetricsObserver(metricsObserver)

        verify { clientMetricsCollector.subscribeToMetrics(metricsObserver) }
    }

    @Test
    fun `removeMetricsObserver should call clientMetricsCollector removeObserver with given observer`() {
        audioVideoController.removeMetricsObserver(metricsObserver)

        verify { clientMetricsCollector.unsubscribeFromMetrics(metricsObserver) }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo`() {
        audioVideoController.startLocalVideo()

        verify { videoClientController.startLocalVideo() }
    }
    @Test
    fun `startLocalVideo should not call videoClientController startLocalVideo when videoMaxResolution is set to None`() {
        audioVideoControllerNone.startLocalVideo()
        verify(exactly = 0) { videoClientController.startLocalVideo() }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo when videoMaxResolution is set to FHD`() {
        every { videoClientController.setMaxBitRateKbps(any()) } just runs
        audioVideoControllerHigh.startLocalVideo()
        verify(exactly = 1) { videoClientController.startLocalVideo() }
        verify(exactly = 1) { videoClientController.setMaxBitRateKbps(any()) }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo with given video config)`() {
        audioVideoController.startLocalVideo(localVideoConfig)

        verify { videoClientController.startLocalVideo(localVideoConfig) }
    }
    @Test
    fun `startLocalVideo should not call videoClientController startLocalVideo with given video config when videoMaxResolution is set to None)`() {
        audioVideoControllerNone.startLocalVideo(localVideoConfig)

        verify(exactly = 0) { videoClientController.startLocalVideo(localVideoConfig) }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo with given video source`() {
        audioVideoController.startLocalVideo(mockVideoSource)

        verify { videoClientController.startLocalVideo(mockVideoSource) }
    }

    @Test
    fun `startLocalVideo should call videoClientController startLocalVideo with given video source and config)`() {
        audioVideoController.startLocalVideo(mockVideoSource, localVideoConfig)

        verify { videoClientController.startLocalVideo(mockVideoSource, localVideoConfig) }
    }

    @Test
    fun `stopLocalVideo should call videoClientController stopLocalVideo`() {
        audioVideoController.stopLocalVideo()

        verify { videoClientController.stopLocalVideo() }
    }

    @Test
    fun `startRemoteVideo should call videoClientController startRemoteVideo`() {
        audioVideoController.startRemoteVideo()

        verify { videoClientController.startRemoteVideo() }
    }

    @Test
    fun `stopRemoteVideo should call videoClientController stopRemoteVideo`() {
        audioVideoController.stopRemoteVideo()

        verify { videoClientController.stopRemoteVideo() }
    }

    @Test
    fun `updateVideoSourceSubscriptions should call videoClientController updateVideoSourceSubscriptions`() {
        val addedOrUpdated = emptyMap<RemoteVideoSource, VideoSubscriptionConfiguration>()
        val removed = emptyArray<RemoteVideoSource>()

        audioVideoController.updateVideoSourceSubscriptions(addedOrUpdated, removed)

        verify { videoClientController.updateVideoSourceSubscriptions(addedOrUpdated, removed) }
    }

    @Test
    fun `promoteToPrimaryMeeting returns success when both audio and video client callback for success`() {
        val audioObserver = slot<PrimaryMeetingPromotionObserver>()
        val videoObserver = slot<PrimaryMeetingPromotionObserver>()
        every { audioClientController.promoteToPrimaryMeeting(any(), observer = capture(audioObserver)) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), observer = capture(videoObserver)) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        audioObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))
        videoObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))

        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK)) }
    }

    @Test
    fun `promoteToPrimaryMeeting returns failure when video fails`() {
        val audioObserver = slot<PrimaryMeetingPromotionObserver>()
        val videoObserver = slot<PrimaryMeetingPromotionObserver>()
        every { audioClientController.promoteToPrimaryMeeting(any(), observer = capture(audioObserver)) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), observer = capture(videoObserver)) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        audioObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))

        videoObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable))
        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable)) }
    }

    @Test
    fun `promoteToPrimaryMeeting returns failure when audio fails`() {
        val audioObserver = slot<PrimaryMeetingPromotionObserver>()
        val videoObserver = slot<PrimaryMeetingPromotionObserver>()
        every { audioClientController.promoteToPrimaryMeeting(any(), observer = capture(audioObserver)) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), observer = capture(videoObserver)) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        audioObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable))
        videoObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))

        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable)) }
    }

    @Test
    fun `promoteToPrimaryMeeting returns failure if either times out`() {
        mockkConstructor(Timer::class)
        val timerTaskSlot = slot<TimerTask>()
        every { anyConstructed<Timer>().schedule(capture(timerTaskSlot), 5000L) } returns Unit
        val audioObserver = slot<PrimaryMeetingPromotionObserver>()
        val videoObserver = slot<PrimaryMeetingPromotionObserver>()
        every { audioClientController.promoteToPrimaryMeeting(any(), observer = capture(audioObserver)) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), observer = capture(videoObserver)) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        videoObserver.captured.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.OK))
        timerTaskSlot.captured.run()

        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingPromotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioInternalServerError)) }
    }

    @Test
    fun `demoteFromPrimaryMeeting calls demoteFromPrimaryMeeting on clients and also calls observer function`() {
        every { audioClientController.promoteToPrimaryMeeting(any(), any()) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), any()) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        audioVideoController.demoteFromPrimaryMeeting()

        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingDemotion(MeetingSessionStatus(MeetingSessionStatusCode.OK)) }
    }

    @Test
    fun `promoteToPrimaryMeeting and a demotion from client will call original observer`() {
        val audioObserver = slot<PrimaryMeetingPromotionObserver>()
        val videoObserver = slot<PrimaryMeetingPromotionObserver>()
        every { audioClientController.promoteToPrimaryMeeting(any(), observer = capture(audioObserver)) } returns Unit
        every { videoClientController.promoteToPrimaryMeeting(any(), observer = capture(videoObserver)) } returns Unit

        audioVideoController.promoteToPrimaryMeeting(mockPrimaryMeetingSessionCredentials, mockPrimaryMeetingPromotionObserver)
        audioObserver.captured.onPrimaryMeetingDemotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable))

        verify { mockPrimaryMeetingPromotionObserver.onPrimaryMeetingDemotion(MeetingSessionStatus(MeetingSessionStatusCode.AudioServiceUnavailable)) }
    }
}
