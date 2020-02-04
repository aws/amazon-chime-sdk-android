package com.amazon.chime.sdkdemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.session.DefaultMeetingSession
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.session.MeetingSessionCredentials
import com.amazon.chime.sdk.session.MeetingSessionURLs
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import com.amazon.chime.sdkdemo.data.MeetingResponse
import com.google.gson.Gson

class InMeetingActivity : AppCompatActivity(),
    DeviceManagementFragment.DeviceManagementEventListener,
    RosterViewFragment.RosterViewEventListener {

    private val logger = ConsoleLogger(LogLevel.INFO)
    private val gson = Gson()
    private lateinit var meetingId: String
    private lateinit var name: String
    private lateinit var audioVideo: AudioVideoFacade

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
                "There was an error starting the meeting. Please try again.",
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
        val rosterViewFragment = RosterViewFragment.newInstance(meetingId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
            .commit()
    }

    override fun onLeaveMeetingClicked() {
        audioVideo.stop()
        onBackPressed()
    }

    fun getAudioVideo(): AudioVideoFacade {
        return audioVideo
    }

    private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null

        val meetingResponse = gson.fromJson(response, MeetingResponse::class.java)
        val meeting = meetingResponse.joinInfo.meeting
        val attendee = meetingResponse.joinInfo.attendee

        val credentials = MeetingSessionCredentials(attendee.attendeeId, attendee.joinToken)
        val urls = MeetingSessionURLs(meeting.mediaPlacement.audioHostUrl)
        return MeetingSessionConfiguration(meeting.meetingId, credentials, urls)
    }
}
