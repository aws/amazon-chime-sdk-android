/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdkdemo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.enums.ObservableMetric
import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import com.amazon.chime.sdkdemo.data.AttendeeInfoResponse
import com.amazon.chime.sdkdemo.data.RosterAttendee
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RosterViewFragment : Fragment(), RealtimeObserver, AudioVideoObserver {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val gson = Gson()
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private lateinit var meetingId: String
    private lateinit var listener: RosterViewEventListener
    private lateinit var audioVideo: AudioVideoFacade

    private val TAG = "RosterViewFragment"

    private lateinit var adapter: RosterAdapter

    companion object {
        fun newInstance(meetingId: String): RosterViewFragment {
            val fragment = RosterViewFragment()

            fragment.arguments =
                Bundle().apply { putString(MeetingHomeActivity.MEETING_ID_KEY, meetingId) }
            return fragment
        }
    }

    interface RosterViewEventListener {
        fun onLeaveMeeting()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is RosterViewEventListener) {
            listener = context
        } else {
            logger.error(TAG, "$context must implement RosterViewEventListener.")
            throw ClassCastException("$context must implement RosterViewEventListener.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_roster_view, container, false)
        val activity = activity as Context

        meetingId = arguments?.getString(MeetingHomeActivity.MEETING_ID_KEY) as String
        audioVideo = (activity as InMeetingActivity).getAudioVideo()

        view.findViewById<Button>(R.id.buttonMute)?.setOnClickListener { muteMeeting() }
        view.findViewById<Button>(R.id.buttonUnmute)?.setOnClickListener { unmuteMeeting() }
        view.findViewById<Button>(R.id.buttonLeave)
            ?.setOnClickListener { listener.onLeaveMeeting() }

        val recyclerViewRoster = view.findViewById<RecyclerView>(R.id.recyclerViewRoster)
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        adapter = RosterAdapter(currentRoster.values, activity)
        recyclerViewRoster.adapter = adapter

        audioVideo.addObserver(this)
        audioVideo.realtimeAddObserver(this)
        audioVideo.start()
        return view
    }

    override fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>) {
        uiScope.launch {
            mutex.withLock {
                val updatedRoster = mutableMapOf<String, RosterAttendee>()
                attendeeVolumes.forEach { (attendeeId, volume) ->
                    currentRoster[attendeeId]?.let {
                        updatedRoster[attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                volume,
                                it.signalStrength
                            )
                    }
                }

                currentRoster.clear()
                currentRoster.putAll(updatedRoster)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>) {
        uiScope.launch {
            mutex.withLock {
                val updatedRoster = mutableMapOf<String, RosterAttendee>()
                attendeeSignalStrength.forEach { (attendeeId, signalStrength) ->
                    logger.info(TAG, "Attendee $attendeeId signalStrength: $signalStrength")

                    currentRoster[attendeeId]?.let {
                        updatedRoster[attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                it.volumeLevel,
                                signalStrength
                            )
                    }
                }

                currentRoster.clear()
                currentRoster.putAll(updatedRoster)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesJoin(attendeeIds: Array<String>) {
        uiScope.launch {
            mutex.withLock {
                val updatedRoster: MutableMap<String, RosterAttendee> = currentRoster.toMutableMap()
                attendeeIds.forEach { attendeeId ->
                    val attendeeName: String = (currentRoster[attendeeId]?.let { it.attendeeName }
                        ?: getAttendeeName(getString(R.string.test_url), attendeeId)) ?: ""

                    updatedRoster[attendeeId] = RosterAttendee(attendeeId, attendeeName)
                }

                currentRoster.clear()
                currentRoster.putAll(updatedRoster)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesLeave(attendeeIds: Array<String>) {
        uiScope.launch {
            mutex.withLock {
                val updatedRoster = mutableMapOf<String, RosterAttendee>()
                currentRoster.forEach { (attendeeId, attendee) ->
                    val notRemoved = !attendeeIds.contains(attendeeId)
                    if (notRemoved) {
                        updatedRoster[attendeeId] = attendee
                    }
                }

                currentRoster.clear()
                currentRoster.putAll(updatedRoster)
                adapter.notifyDataSetChanged()
            }
        }
    }

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

    private fun muteMeeting() {
        audioVideo.realtimeLocalMute()
    }

    private fun unmuteMeeting() {
        audioVideo.realtimeLocalUnmute()
    }

    override fun onAudioVideoStartConnecting(reconnecting: Boolean) =
        notify("Audio started connecting. reconnecting: $reconnecting")

    override fun onAudioVideoStart(reconnecting: Boolean) =
        notify("Audio successfully started. reconnecting: $reconnecting")

    override fun onAudioVideoStop(sessionStatus: MeetingSessionStatus) {
        notify("Audio stopped for reason: ${sessionStatus.statusCode}")
        listener.onLeaveMeeting()
    }

    override fun onAudioReconnectionCancel() = notify("Audio cancelled reconnecting")
    override fun onConnectionRecovered() = notify("Connection quality has recovered")
    override fun onConnectionBecamePoor() = notify("Connection quality has become poor")

    override fun onMetricsReceive(metrics: Map<ObservableMetric, Any>) {
        logger.info(TAG, "Media metrics received: $metrics")
    }

    private fun notify(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        logger.info(TAG, message)
    }
}
