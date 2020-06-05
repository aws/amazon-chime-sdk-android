/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.data.JoinMeetingResponse
import com.google.gson.Gson

class InMeetingActivity : AppCompatActivity(),
    DeviceManagementFragment.DeviceManagementEventListener,
    RosterViewFragment.RosterViewEventListener {

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val gson = Gson()
    private val meetingSessionModel: MeetingSessionModel by lazy { ViewModelProvider(this)[MeetingSessionModel::class.java] }
    private lateinit var meetingId: String
    private lateinit var name: String

    private val TAG = "InMeetingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_meeting)
        meetingId = intent.getStringExtra(MeetingHomeActivity.MEETING_ID_KEY) as String
        name = intent.getStringExtra(MeetingHomeActivity.NAME_KEY) as String

        if (savedInstanceState == null) {
            val meetingResponseJson =
                intent.getStringExtra(MeetingHomeActivity.MEETING_RESPONSE_KEY) as String
            val sessionConfig = createSessionConfiguration(meetingResponseJson)
            val meetingSession = sessionConfig?.let {
                logger.info(TAG, "Creating meeting session for meeting Id: $meetingId")
                DefaultMeetingSession(
                    it,
                    logger,
                    applicationContext
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

            val deviceManagementFragment = DeviceManagementFragment.newInstance(meetingId, name)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, deviceManagementFragment, "deviceManagement")
                .commit()
        }
    }

    override fun onJoinMeetingClicked() {
        val rosterViewFragment = RosterViewFragment.newInstance(meetingId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
            .commit()
    }

    override fun onLeaveMeeting() {
        onBackPressed()
    }

    override fun onBackPressed() {
        meetingSessionModel.audioVideo.stop()
        super.onBackPressed()
    }

    fun getAudioVideo(): AudioVideoFacade = meetingSessionModel.audioVideo

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

    class MeetingSessionModel : ViewModel() {
        private lateinit var meetingSession: MeetingSession

        fun setMeetingSession(meetingSession: MeetingSession) {
            this.meetingSession = meetingSession
        }

        val audioVideo: AudioVideoFacade
            get() = meetingSession.audioVideo
    }
}
