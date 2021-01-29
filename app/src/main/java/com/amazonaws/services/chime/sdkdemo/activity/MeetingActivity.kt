/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
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
    private lateinit var name: String

    private var cachedDevice: MediaDevice? = null

    private val TAG = "InMeetingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)
        meetingId = intent.getStringExtra(HomeActivity.MEETING_ID_KEY) as String
        name = intent.getStringExtra(HomeActivity.NAME_KEY) as String

        if (savedInstanceState == null) {
            val meetingResponseJson =
                intent.getStringExtra(HomeActivity.MEETING_RESPONSE_KEY) as String
            val sessionConfig = createSessionConfiguration(meetingResponseJson)
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
                meetingSessionModel.setMeetingSession(meetingSession)
            }

            val surfaceTextureCaptureSourceFactory = DefaultSurfaceTextureCaptureSourceFactory(logger, meetingSessionModel.eglCoreFactory)
            meetingSessionModel.cameraCaptureSource = DefaultCameraCaptureSource(applicationContext, logger, surfaceTextureCaptureSourceFactory).apply {
                eventAnalyticsController = meetingSession?.eventAnalyticsController
            }
            meetingSessionModel.cpuVideoProcessor = CpuVideoProcessor(logger, meetingSessionModel.eglCoreFactory)
            meetingSessionModel.gpuVideoProcessor = GpuVideoProcessor(logger, meetingSessionModel.eglCoreFactory)

            val deviceManagementFragment = DeviceManagementFragment.newInstance(meetingId, name)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, deviceManagementFragment, "deviceManagement")
                .commit()
        }
    }

    override fun onJoinMeetingClicked() {
        val rosterViewFragment = MeetingFragment.newInstance(meetingId)
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
        meetingSessionModel.screenShareManager?.stop()
        meetingSessionModel.screenShareManager?.release()
    }

    fun getAudioVideo(): AudioVideoFacade = meetingSessionModel.audioVideo

    fun getMeetingSessionConfiguration(): MeetingSessionConfiguration = meetingSessionModel.configuration

    fun getMeetingSessionCredentials(): MeetingSessionCredentials = meetingSessionModel.credentials

    fun getCachedDevice(): MediaDevice? = cachedDevice
    fun resetCachedDevice() {
        cachedDevice = null
    }
    fun getEglCoreFactory(): EglCoreFactory = meetingSessionModel.eglCoreFactory

    fun getCameraCaptureSource(): CameraCaptureSource = meetingSessionModel.cameraCaptureSource

    fun getGpuVideoProcessor(): GpuVideoProcessor = meetingSessionModel.gpuVideoProcessor

    fun getCpuVideoProcessor(): CpuVideoProcessor = meetingSessionModel.cpuVideoProcessor

    fun getScreenShareManager(): ScreenShareManager? = meetingSessionModel.screenShareManager

    fun setScreenShareManager(screenShareManager: ScreenShareManager?) {
        meetingSessionModel.screenShareManager = screenShareManager
    }

    private fun urlRewriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null

        return try {
            val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
            MeetingSessionConfiguration(
                CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
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
