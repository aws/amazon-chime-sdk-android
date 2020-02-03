package com.amazon.chime.sdkdemo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.session.DefaultMeetingSession
import com.amazon.chime.sdk.session.MeetingSessionConfiguration
import com.amazon.chime.sdk.session.MeetingSessionCredentials
import com.amazon.chime.sdk.session.MeetingSessionURLs
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import com.amazon.chime.sdkdemo.data.AttendeeInfoResponse
import com.amazon.chime.sdkdemo.data.MeetingResponse
import com.amazon.chime.sdkdemo.data.RosterAttendee
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.android.synthetic.main.activity_in_meeting.recyclerViewRoster
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InMeetingActivity : AppCompatActivity(), RealtimeObserver {

    private val logger = ConsoleLogger(LogLevel.INFO)
    private val gson = Gson()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private lateinit var meetingId: String
    private lateinit var meetingSession: DefaultMeetingSession
    private lateinit var audioVideo: AudioVideoFacade

    private val TAG = "InMeetingActivity"

    private lateinit var adapter: RosterAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_meeting)

        findViewById<Button>(R.id.buttonLeave)?.setOnClickListener { leaveMeeting() }
        findViewById<Button>(R.id.buttonMute)?.setOnClickListener { muteMeeting() }
        findViewById<Button>(R.id.buttonUnmute)?.setOnClickListener { unmuteMeeting() }

        linearLayoutManager = LinearLayoutManager(this)

        recyclerViewRoster.layoutManager = linearLayoutManager
        adapter = RosterAdapter(currentRoster.values)
        recyclerViewRoster.adapter = adapter

        meetingId = intent.getStringExtra(MeetingHomeActivity.MEETING_ID_KEY) as String

        val meetingResponseJson =
            intent.getStringExtra(MeetingHomeActivity.MEETING_RESPONSE_KEY) as String
        val sessionConfig = createSessionConfiguration(meetingResponseJson)
        sessionConfig?.let {
            initializeMeetingSession(sessionConfig)
            audioVideo.start()
        }
    }

    private fun initializeMeetingSession(configuration: MeetingSessionConfiguration) {
        meetingSession =
            DefaultMeetingSession(configuration, logger, applicationContext)
        audioVideo = meetingSession.audioVideo
        setupSubscriptionToAttendeeChangeHandler()
    }

    private fun setupSubscriptionToAttendeeChangeHandler() = audioVideo.realtimeAddObserver(this)

    private suspend fun getAttendeeName(
        meetingUrl: String,
        attendeeId: String
    ): String? {
        return withContext(ioDispatcher) {
            val serverUrl =
                URL("${meetingUrl}attendee?title=$meetingId&attendee=$attendeeId")
            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }
                    gson.fromJson(
                        response.toString(),
                        AttendeeInfoResponse::class.java
                    ).attendeeInfo.name
                }
            } catch (exception: Exception) {
                logger.error(TAG, "Error getting attendee info. Exception: ${exception.message}")
                null
            }
        }
    }

    private fun leaveMeeting() {
        audioVideo.stop()
        onBackPressed()
    }

    private fun muteMeeting() {
        audioVideo.realtimeLocalMute()
    }

    private fun unmuteMeeting() {
        audioVideo.realtimeLocalUnmute()
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

    override fun onVolumeChange(attendeeVolumes: Map<String, Int>) {
        uiScope.launch {
            val updatedRoster = mutableMapOf<String, RosterAttendee>()
            for ((attendeeId, volume) in attendeeVolumes) {
                val attendeeName: String =
                    if (currentRoster.containsKey(attendeeId) &&
                        currentRoster.getValue(attendeeId).attendeeName.isNotBlank()
                    ) currentRoster.getValue(attendeeId).attendeeName
                    else getAttendeeName(getString(R.string.test_url), attendeeId) ?: ""

                updatedRoster[attendeeId] =
                    RosterAttendee(attendeeName, volume)
            }
            currentRoster.clear()
            currentRoster.putAll(updatedRoster)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, Int>) {
        // Do nothing for now
    }
}
