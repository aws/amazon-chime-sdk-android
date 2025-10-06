/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioDeviceCapabilities
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur.BackgroundBlurConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur.BackgroundBlurVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement.BackgroundReplacementVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MediaPlacement
import com.amazonaws.services.chime.sdk.meetings.session.Meeting
import com.amazonaws.services.chime.sdk.meetings.session.MeetingFeatures
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.JoinMeetingResponse
import com.amazonaws.services.chime.sdkdemo.device.ScreenShareManager
import com.amazonaws.services.chime.sdkdemo.fragment.DeviceManagementFragment
import com.amazonaws.services.chime.sdkdemo.fragment.MeetingFragment
import com.amazonaws.services.chime.sdkdemo.model.MeetingSessionModel
import com.amazonaws.services.chime.sdkdemo.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdkdemo.utils.GpuVideoProcessor
import com.google.gson.Gson

class MeetingActivity : AppCompatActivity(),
    DeviceManagementFragment.DeviceManagementEventListener,
    MeetingFragment.RosterViewEventListener {

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val gson = Gson()
    private val meetingSessionModel: MeetingSessionModel by lazy { ViewModelProvider(this)[MeetingSessionModel::class.java] }

    private lateinit var meetingId: String
    private var primaryExternalMeetingId: String? = null
    private lateinit var name: String
    private lateinit var audioVideoConfig: AudioVideoConfiguration
    private lateinit var meetingEndpointUrl: String

    private var cachedDevice: MediaDevice? = null

    private val TAG = "InMeetingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        meetingId = intent.extras?.getString(HomeActivity.MEETING_ID_KEY) as String
        name = intent.extras?.getString(HomeActivity.NAME_KEY) as String
        val audioMode = intent.extras?.getInt(HomeActivity.AUDIO_MODE_KEY)?.let { intValue ->
            AudioMode.from(intValue, defaultAudioMode = AudioMode.Stereo48K)
        } ?: AudioMode.Stereo48K
        val audioDeviceCapabilities = intent.extras?.get(HomeActivity.AUDIO_DEVICE_CAPABILITIES_KEY) as? AudioDeviceCapabilities ?: AudioDeviceCapabilities.InputAndOutput
        val enableAudioRedundancy = intent.extras?.getBoolean(HomeActivity.ENABLE_AUDIO_REDUNDANCY_KEY) as Boolean
        val reconnectTimeoutMs = intent.extras?.getInt(HomeActivity.RECONNECT_TIMEOUT_MS) as Int
        audioVideoConfig = AudioVideoConfiguration(audioMode = audioMode, audioDeviceCapabilities = audioDeviceCapabilities, enableAudioRedundancy = enableAudioRedundancy, reconnectTimeoutMs = reconnectTimeoutMs)
        meetingEndpointUrl = intent.extras?.getString(HomeActivity.MEETING_ENDPOINT_KEY) as String

        if (savedInstanceState == null) {
            val meetingResponseJson =
                intent.extras?.getString(HomeActivity.MEETING_RESPONSE_KEY) as String
            val sessionConfig =
                createSessionConfigurationAndExtractPrimaryMeetingInformation(meetingResponseJson)
            val meetingSession = sessionConfig?.let {
                logger.info(TAG, "Creating meeting session for meeting Id: $meetingId")

                DefaultMeetingSession(
                    it,
                    logger,
                    applicationContext,
                    // Note if the following isn't provided app will (as expected) crash if we use custom video source
                    // since an EglCoreFactory will be internal created and will be using a different shared EGLContext.
                    // However the internal default capture would work fine, since it is initialized using
                    // that internally created default EglCoreFactory, and can be smoke tested by removing this
                    // argument and toggling use of custom video source before starting video
                    meetingSessionModel.eglCoreFactory
                )
            }

            if (meetingSession == null) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.user_notification_meeting_start_error),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                meetingSessionModel.meetingSession = meetingSession
                meetingSessionModel.primaryExternalMeetingId = primaryExternalMeetingId
            }

            val surfaceTextureCaptureSourceFactory = DefaultSurfaceTextureCaptureSourceFactory(
                logger,
                meetingSessionModel.eglCoreFactory
            )
            meetingSessionModel.cameraCaptureSource = DefaultCameraCaptureSource(
                applicationContext,
                logger,
                surfaceTextureCaptureSourceFactory,
                meetingSession?.eventAnalyticsController
            )
            // Add a new parameter for DefaultCameraCaptureSource (videoMaxResolution)
            var resolution: VideoResolution = VideoResolution.VideoResolutionHD
            meetingSession?.let {
                resolution = it.configuration.features.videoMaxResolution
            }
            meetingSessionModel.cameraCaptureSource.setMaxResolution(resolution)

            meetingSessionModel.cpuVideoProcessor =
                CpuVideoProcessor(logger, meetingSessionModel.eglCoreFactory)
            meetingSessionModel.gpuVideoProcessor =
                GpuVideoProcessor(logger, meetingSessionModel.eglCoreFactory)
            meetingSessionModel.backgroundBlurVideoFrameProcessor =
                BackgroundBlurVideoFrameProcessor(
                    logger,
                    meetingSessionModel.eglCoreFactory,
                    applicationContext,
                    BackgroundBlurConfiguration()
                )
            meetingSessionModel.backgroundReplacementVideoFrameProcessor =
                BackgroundReplacementVideoFrameProcessor(
                    logger,
                    meetingSessionModel.eglCoreFactory,
                    applicationContext,
                    null
                )

            val deviceManagementFragment =
                DeviceManagementFragment.newInstance(meetingId, name, audioVideoConfig)
            deviceManagementFragment.setVideoMaxResolution(meetingSessionModel.meetingSession.configuration.features.videoMaxResolution)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, deviceManagementFragment, "deviceManagement")
                .commit()
        }
    }

    override fun onJoinMeetingClicked() {
        val rosterViewFragment =
            MeetingFragment.newInstance(meetingId, audioVideoConfig, meetingEndpointUrl)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
            .commit()
    }

    override fun onCachedDeviceSelected(mediaDevice: MediaDevice) {
        cachedDevice = mediaDevice
    }

    override fun onLeaveMeeting() {
        onBackPressed()
    }

    override fun onDestroy() {
        if (isFinishing) {
            cleanup()
        }
        super.onDestroy()
    }

    private fun cleanup() {
        meetingSessionModel.audioVideo.stopLocalVideo()
        meetingSessionModel.audioVideo.stopRemoteVideo()
        meetingSessionModel.audioVideo.stopContentShare()
        meetingSessionModel.audioVideo.stop()
        meetingSessionModel.cameraCaptureSource.stop()
        meetingSessionModel.gpuVideoProcessor.release()
        meetingSessionModel.cpuVideoProcessor.release()
        meetingSessionModel.backgroundBlurVideoFrameProcessor.release()
        meetingSessionModel.backgroundReplacementVideoFrameProcessor.release()
        meetingSessionModel.screenShareManager?.stop()
        meetingSessionModel.screenShareManager?.release()
    }

    fun getAudioVideo(): AudioVideoFacade = meetingSessionModel.audioVideo

    fun getMeetingSessionConfiguration(): MeetingSessionConfiguration =
        meetingSessionModel.configuration

    fun getMeetingSessionCredentials(): MeetingSessionCredentials = meetingSessionModel.credentials

    fun getPrimaryExternalMeetingId(): String? = meetingSessionModel.primaryExternalMeetingId

    fun getCachedDevice(): MediaDevice? = cachedDevice
    fun resetCachedDevice() {
        cachedDevice = null
    }

    fun getEglCoreFactory(): EglCoreFactory = meetingSessionModel.eglCoreFactory

    fun getCameraCaptureSource(): CameraCaptureSource = meetingSessionModel.cameraCaptureSource

    fun getGpuVideoProcessor(): GpuVideoProcessor = meetingSessionModel.gpuVideoProcessor

    fun getCpuVideoProcessor(): CpuVideoProcessor = meetingSessionModel.cpuVideoProcessor

    fun getBackgroundBlurVideoFrameProcessor(): BackgroundBlurVideoFrameProcessor =
        meetingSessionModel.backgroundBlurVideoFrameProcessor

    fun getBackgroundReplacementVideoFrameProcessor(): BackgroundReplacementVideoFrameProcessor =
        meetingSessionModel.backgroundReplacementVideoFrameProcessor

    fun getScreenShareManager(): ScreenShareManager? = meetingSessionModel.screenShareManager

    fun setScreenShareManager(screenShareManager: ScreenShareManager?) {
        meetingSessionModel.screenShareManager = screenShareManager
    }

    private fun urlRewriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    private fun createSessionConfigurationAndExtractPrimaryMeetingInformation(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null

        return try {
            val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
            primaryExternalMeetingId = joinMeetingResponse.joinInfo.primaryExternalMeetingId
            val meetingResp = joinMeetingResponse.joinInfo.meetingResponse.meeting
            val externalMeetingId: String? = meetingResp.ExternalMeetingId
            val mediaPlacement: MediaPlacement = meetingResp.MediaPlacement
            val mediaRegion: String = meetingResp.MediaRegion
            val meetingId: String = meetingResp.MeetingId
            val meetingFeatures: MeetingFeatures = MeetingFeatures(meetingResp.MeetingFeatures?.Video?.MaxResolution, meetingResp.MeetingFeatures?.Content?.MaxResolution)
            val meeting =
                Meeting(
                    externalMeetingId,
                    mediaPlacement,
                    mediaRegion,
                    meetingId,
                    meetingFeatures
                )
            MeetingSessionConfiguration(
                CreateMeetingResponse(meeting),
                CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
                ::urlRewriter
            )
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Error creating session configuration: ${exception.localizedMessage}"
            )
            null
        }
    }
}
