/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdkdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazon.chime.sdk.media.AudioVideoFacade
import com.amazon.chime.sdk.media.enums.ObservableMetric
import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileObserver
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileState
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import com.amazon.chime.sdkdemo.data.AttendeeInfoResponse
import com.amazon.chime.sdkdemo.data.RosterAttendee
import com.amazon.chime.sdkdemo.data.VideoCollectionTile
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

class RosterViewFragment : Fragment(), RealtimeObserver, AudioVideoObserver, VideoTileObserver {

    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val gson = Gson()
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private val currentVideoTiles = mutableMapOf<Int, VideoCollectionTile>()
    private val nextVideoTiles = LinkedHashMap<Int, VideoCollectionTile>()
    private var isMuted = false
    private var isCameraOn = false
    private lateinit var meetingId: String
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var listener: RosterViewEventListener

    private val MAX_TILE_COUNT = 2
    private val LOCAL_TILE_ID = 0
    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val TAG = "RosterViewFragment"

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.CAMERA
    )

    private lateinit var buttonMute: ImageButton
    private lateinit var buttonVideo: ImageButton
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoTileAdapter: VideoCollectionTileAdapter

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

        view.findViewById<TextView>(R.id.textViewMeetingId)?.text = meetingId

        buttonMute = view.findViewById(R.id.buttonMute)
        buttonMute.setOnClickListener { toggleMuteMeeting() }

        buttonVideo = view.findViewById(R.id.buttonVideo)
        buttonVideo.setOnClickListener { toggleVideo() }

        view.findViewById<ImageButton>(R.id.buttonLeave)
            ?.setOnClickListener { listener.onLeaveMeeting() }

        val recyclerViewVideoCollection =
            view.findViewById<RecyclerView>(R.id.recyclerViewVideoCollection)
        recyclerViewVideoCollection.layoutManager = LinearLayoutManager(activity)
        videoTileAdapter = VideoCollectionTileAdapter(currentVideoTiles.values, audioVideo, context)
        recyclerViewVideoCollection.adapter = videoTileAdapter

        val recyclerViewRoster = view.findViewById<RecyclerView>(R.id.recyclerViewRoster)
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        rosterAdapter = RosterAdapter(currentRoster.values)
        recyclerViewRoster.adapter = rosterAdapter

        audioVideo.addObserver(this)
        audioVideo.realtimeAddObserver(this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.start()
        return view
    }

    override fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>) {
        uiScope.launch {
            mutex.withLock {
                attendeeVolumes.forEach { (attendeeId, volume) ->
                    currentRoster[attendeeId]?.let {
                        currentRoster[attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                volume,
                                it.signalStrength
                            )
                    }
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>) {
        uiScope.launch {
            mutex.withLock {
                attendeeSignalStrength.forEach { (attendeeId, signalStrength) ->
                    logger.info(TAG, "Attendee $attendeeId signalStrength: $signalStrength")

                    currentRoster[attendeeId]?.let {
                        currentRoster[attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                it.volumeLevel,
                                signalStrength
                            )
                    }
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesJoin(attendeeIds: Array<String>) {
        uiScope.launch {
            mutex.withLock {
                attendeeIds.forEach {
                    currentRoster.getOrPut(
                        it,
                        {
                            RosterAttendee(
                                it,
                                getAttendeeName(getString(R.string.test_url), it) ?: ""
                            )
                        })
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesLeave(attendeeIds: Array<String>) {
        uiScope.launch {
            mutex.withLock {
                attendeeIds.forEach { currentRoster.remove(it) }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesMute(attendeeIds: Array<String>) {
        attendeeIds.forEach {
            logger.info(TAG, "Attendee $it muted")
        }
    }

    override fun onAttendeesUnmute(attendeeIds: Array<String>) {
        attendeeIds.forEach {
            logger.info(TAG, "Attendee $it unmuted")
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

    private fun toggleMuteMeeting() {
        if (isMuted) unmuteMeeting() else muteMeeting()
        isMuted = !isMuted
    }

    private fun muteMeeting() {
        audioVideo.realtimeLocalMute()
        buttonMute.setImageResource(R.drawable.button_mute_on)
    }

    private fun unmuteMeeting() {
        audioVideo.realtimeLocalUnmute()
        buttonMute.setImageResource(R.drawable.button_mute)
    }

    private fun toggleVideo() {
        if (isCameraOn) stopCamera() else startCamera()
        isCameraOn = !isCameraOn
    }

    private fun startCamera() {
        if (hasPermissionsAlready()) {
            startLocalVideo()
        } else {
            requestPermissions(
                WEBRTC_PERM,
                WEBRTC_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startLocalVideo() {
        audioVideo.startLocalVideo()
        buttonVideo.setImageResource(R.drawable.button_video_on)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    Toast.makeText(
                        context!!,
                        getString(R.string.user_notification_permission_error),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    startLocalVideo()
                }
                return
            }
        }
    }

    private fun hasPermissionsAlready(): Boolean {
        return WEBRTC_PERM.all {
            ContextCompat.checkSelfPermission(context!!, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun stopCamera() {
        audioVideo.stopLocalVideo()
        buttonVideo.setImageResource(R.drawable.button_video)
    }

    private fun showVideoTile(tileState: VideoTileState) {
        currentVideoTiles[tileState.tileId] = createVideoCollectionTile(tileState)
        videoTileAdapter.notifyDataSetChanged()
    }

    private fun canShowMoreRemoteVideoTile(): Boolean {
        // Current max amount of tiles should preserve one spot for local video
        val currentMax =
            if (currentVideoTiles.containsKey(LOCAL_TILE_ID)) MAX_TILE_COUNT else MAX_TILE_COUNT - 1
        return currentVideoTiles.size < currentMax
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        attendeeId?.let {
            val attendeeName = currentRoster[attendeeId]?.attendeeName ?: ""
            return VideoCollectionTile(attendeeName, tileState)
        }

        return VideoCollectionTile("", tileState)
    }

    override fun onRemoveVideoTile(tileState: VideoTileState) {
        uiScope.launch {
            val tileId: Int = tileState.tileId

            logger.info(
                TAG,
                "Video track removed, titleId: $tileId, attendeeId: ${tileState.attendeeId}"
            )
            if (currentVideoTiles.containsKey(tileId)) {
                audioVideo.unbindVideoView(tileId)
                currentVideoTiles.remove(tileId)
                // Show next video tileState if available
                if (nextVideoTiles.isNotEmpty() && canShowMoreRemoteVideoTile()) {
                    val nextTileState: VideoTileState =
                        nextVideoTiles.entries.iterator().next().value.videoTileState
                    showVideoTile(nextTileState)
                    nextVideoTiles.remove(nextTileState.tileId)
                }
                videoTileAdapter.notifyDataSetChanged()
            } else {
                // Clean up removed tiles
                if (nextVideoTiles.containsKey(tileId)) {
                    nextVideoTiles.remove(tileId)
                }
            }
        }
    }

    override fun onAudioClientConnecting(reconnecting: Boolean) =
        notify("Audio started connecting. reconnecting: $reconnecting")

    override fun onAudioClientStart(reconnecting: Boolean) =
        notify("Audio successfully started. reconnecting: $reconnecting")

    override fun onAudioClientStop(sessionStatus: MeetingSessionStatus) {
        notify("Audio stopped for reason: ${sessionStatus.statusCode}")
        listener.onLeaveMeeting()
    }

    override fun onAudioClientReconnectionCancel() = notify("Audio cancelled reconnecting")

    override fun onConnectionRecover() = notify("Connection quality has recovered")

    override fun onConnectionBecomePoor() = notify("Connection quality has become poor")

    override fun onVideoClientConnecting() = notify("Video started connecting.")

    override fun onVideoClientStart() = notify("Video successfully started.")

    override fun onVideoClientStop(sessionStatus: MeetingSessionStatus) =
        notify("Video stopped for reason: ${sessionStatus.statusCode}")

    override fun onAddVideoTile(tileState: VideoTileState) {
        uiScope.launch {
            logger.info(
                TAG,
                "Video track added, titleId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}"
            )
            // For local video, should show it anyway
            if (tileState.isLocalTile) {
                showVideoTile(tileState)
            } else if (!currentVideoTiles.containsKey(tileState.tileId)) {
                if (canShowMoreRemoteVideoTile()) {
                    showVideoTile(tileState)
                } else {
                    nextVideoTiles[tileState.tileId] = createVideoCollectionTile(tileState)
                }
            }
        }
    }

    override fun onMetricsReceive(metrics: Map<ObservableMetric, Any>) {
        logger.debug(TAG, "Media metrics received: $metrics")
    }

    private fun notify(message: String) {
        uiScope.launch {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            logger.info(TAG, message)
        }
    }
}
