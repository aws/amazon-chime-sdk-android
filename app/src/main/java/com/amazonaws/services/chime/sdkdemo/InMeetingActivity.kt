/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.data.MeetingResponse
import com.google.gson.Gson

class InMeetingActivity : AppCompatActivity(),
    DeviceManagementFragment.DeviceManagementEventListener,
    RosterViewFragment.RosterViewEventListener {

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val gson = Gson()
    private lateinit var meetingId: String
    private lateinit var name: String
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var rosterViewFragment: RosterViewFragment

    private val TAG = "InMeetingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_meeting)
        meetingId = intent.getStringExtra(MeetingHomeActivity.MEETING_ID_KEY) as String
        name = intent.getStringExtra(MeetingHomeActivity.NAME_KEY) as String

        val meetingResponseJson =
            intent.getStringExtra(MeetingHomeActivity.MEETING_RESPONSE_KEY) as String
        val sessionConfig = createSessionConfiguration(meetingResponseJson)
        val meetingSession = sessionConfig?.let {
            logger.info(TAG, "Creating meeting session for meeting Id: $meetingId")
            DefaultMeetingSession(
                sessionConfig,
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
            onBackPressed()
        } else {
            audioVideo = meetingSession.audioVideo
        }

        if (savedInstanceState == null) {
            val deviceManagementFragment = DeviceManagementFragment.newInstance(meetingId, name)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, deviceManagementFragment, "deviceManagement")
                .commit()
        }
    }

    override fun onJoinMeetingClicked() {
        rosterViewFragment = RosterViewFragment.newInstance(meetingId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
            .commit()
    }

    override fun onLeaveMeeting() {
        onBackPressed()
    }

    override fun onBackPressed() {
        audioVideo.stop()
        audioVideo.removeActiveSpeakerObserver(rosterViewFragment)
        super.onBackPressed()
    }

    fun getAudioVideo(): AudioVideoFacade = audioVideo

    private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null

        return try {
            val meetingResponse = gson.fromJson(response, MeetingResponse::class.java)
            MeetingSessionConfiguration(
                CreateMeetingResponse(meetingResponse.joinInfo.meeting),
                CreateAttendeeResponse(meetingResponse.joinInfo.attendee)
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
