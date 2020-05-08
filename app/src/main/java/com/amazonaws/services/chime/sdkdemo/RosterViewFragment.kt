/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.DefaultActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.ObservableMetric
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RosterViewFragment : Fragment(),
    RealtimeObserver, AudioVideoObserver, VideoTileObserver,
    MetricsObserver, ActiveSpeakerObserver {
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private val currentVideoTiles = mutableMapOf<Int, VideoCollectionTile>()
    private val currentScreenTiles = mutableMapOf<Int, VideoCollectionTile>()
    private val nextVideoTiles = LinkedHashMap<Int, VideoCollectionTile>()
    private var isMuted = false
    private var isCameraOn = false
    private lateinit var meetingId: String
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var listener: RosterViewEventListener
    override val scoreCallbackIntervalMs: Int? get() = 1000

    private val MAX_TILE_COUNT = 2
    private val LOCAL_TILE_ID = 0
    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val TAG = "RosterViewFragment"

    // Check if attendee Id contains this at the end to identify content share
    private val CONTENT_DELIMITER = "#content"

    // Append to attendee name if it's for content share
    private val CONTENT_NAME_SUFFIX = "<<Content>>"

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.CAMERA
    )

    enum class SubTab(val position: Int) {
        Attendee(0),
        Video(1),
        Screen(2)
    }

    private lateinit var buttonMute: ImageButton
    private lateinit var buttonVideo: ImageButton
    private lateinit var recyclerViewRoster: RecyclerView
    private lateinit var recyclerViewVideoCollection: RecyclerView
    private lateinit var recyclerViewScreenShareCollection: RecyclerView
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoTileAdapter: VideoCollectionTileAdapter
    private lateinit var screenTileAdapter: VideoCollectionTileAdapter
    private lateinit var tabLayout: TabLayout

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

        setupSubTabs(view)

        audioVideo.addAudioVideoObserver(this)
        audioVideo.addMetricsObserver(this)
        audioVideo.addRealtimeObserver(this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.start()
        audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), this)
        return view
    }

    private fun setupSubTabs(view: View) {
        recyclerViewRoster = view.findViewById(R.id.recyclerViewRoster)
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        rosterAdapter = RosterAdapter(currentRoster.values)
        recyclerViewRoster.adapter = rosterAdapter

        recyclerViewVideoCollection =
            view.findViewById(R.id.recyclerViewVideoCollection)
        recyclerViewVideoCollection.layoutManager = LinearLayoutManager(activity)
        videoTileAdapter = VideoCollectionTileAdapter(currentVideoTiles.values, audioVideo, context)
        recyclerViewVideoCollection.adapter = videoTileAdapter

        recyclerViewScreenShareCollection =
            view.findViewById(R.id.recyclerViewScreenShareCollection)
        recyclerViewScreenShareCollection.layoutManager = LinearLayoutManager(activity)
        screenTileAdapter =
            VideoCollectionTileAdapter(currentScreenTiles.values, audioVideo, context)
        recyclerViewScreenShareCollection.adapter = screenTileAdapter

        tabLayout = view.findViewById(R.id.tabLayoutRosterView)
        SubTab.values()
            .forEach { tabLayout.addTab(tabLayout.newTab().setText(it.name).setContentDescription("${it.name} Tab")) }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTabAt(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun showTabAt(index: Int) {
        when (index) {
            SubTab.Attendee.position -> {
                recyclerViewRoster.visibility = View.VISIBLE
                recyclerViewVideoCollection.visibility = View.GONE
                recyclerViewScreenShareCollection.visibility = View.GONE
                audioVideo.stopRemoteVideo()
            }
            SubTab.Video.position -> {
                recyclerViewRoster.visibility = View.GONE
                recyclerViewVideoCollection.visibility = View.VISIBLE
                recyclerViewScreenShareCollection.visibility = View.GONE
                audioVideo.startRemoteVideo()
            }
            SubTab.Screen.position -> {
                recyclerViewRoster.visibility = View.GONE
                recyclerViewVideoCollection.visibility = View.GONE
                recyclerViewScreenShareCollection.visibility = View.VISIBLE
                audioVideo.startRemoteVideo()
            }

            else -> return
        }
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        uiScope.launch {
            mutex.withLock {
                volumeUpdates.forEach { (attendeeInfo, volumeLevel) ->
                    currentRoster[attendeeInfo.attendeeId]?.let {
                        currentRoster[attendeeInfo.attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                volumeLevel,
                                it.signalStrength,
                                it.isActiveSpeaker
                            )
                    }
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        uiScope.launch {
            mutex.withLock {
                signalUpdates.forEach { (attendeeInfo, signalStrength) ->
                    currentRoster[attendeeInfo.attendeeId]?.let {
                        currentRoster[attendeeInfo.attendeeId] =
                            RosterAttendee(
                                it.attendeeId,
                                it.attendeeName,
                                it.volumeLevel,
                                signalStrength,
                                it.isActiveSpeaker
                            )
                    }
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            mutex.withLock {
                attendeeInfo.forEach { (attendeeId, externalUserId) ->
                    currentRoster.getOrPut(
                        attendeeId,
                        {
                            RosterAttendee(
                                attendeeId,
                                getAttendeeName(attendeeId, externalUserId)
                            )
                        })
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            mutex.withLock {
                attendeeInfo.forEach { (attendeeId, _) -> currentRoster.remove(attendeeId) }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            logger.info(
                TAG,
                "Attendee with attendeeId $attendeeId and externalUserId $externalUserId muted"
            )
        }
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            logger.info(
                TAG,
                "Attendee with attendeeId $attendeeId and externalUserId $externalUserId unmuted"
            )
        }
    }

    override fun onActiveSpeakerDetected(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            mutex.withLock {
                var needUpdate = false
                val activeSpeakers = attendeeInfo.map { it.attendeeId }.toSet()
                currentRoster.values.forEach { attendee ->
                    if (activeSpeakers.contains(attendee.attendeeId) != attendee.isActiveSpeaker) {
                        currentRoster[attendee.attendeeId] =
                            RosterAttendee(
                                attendee.attendeeId,
                                attendee.attendeeName,
                                attendee.volumeLevel,
                                attendee.signalStrength,
                                !attendee.isActiveSpeaker
                            )
                        needUpdate = true
                    }
                }

                if (needUpdate) {
                    rosterAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onActiveSpeakerScoreChanged(scores: Map<AttendeeInfo, Double>) {
        logger.debug(TAG, "Active Speakers scores are: $scores")
    }

    private fun getAttendeeName(attendeeId: String, externalUserId: String): String {
        val attendeeName = externalUserId.split('#')[1]

        return if (attendeeId.endsWith(CONTENT_DELIMITER)) {
            "$attendeeName $CONTENT_NAME_SUFFIX"
        } else {
            attendeeName
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
        if (tileState.isContent) {
            currentScreenTiles[tileState.tileId] = createVideoCollectionTile(tileState)
            screenTileAdapter.notifyDataSetChanged()
        } else {
            currentVideoTiles[tileState.tileId] = createVideoCollectionTile(tileState)
            videoTileAdapter.notifyDataSetChanged()
        }
    }

    private fun canShowMoreRemoteVideoTile(): Boolean {
        // Current max amount of tiles should preserve one spot for local video
        val currentMax =
            if (currentVideoTiles.containsKey(LOCAL_TILE_ID)) MAX_TILE_COUNT else MAX_TILE_COUNT - 1
        return currentVideoTiles.size < currentMax
    }

    private fun canShowMoreRemoteScreenTile(): Boolean {
        // only show 1 screen share tile
        return currentScreenTiles.isEmpty()
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        attendeeId?.let {
            val attendeeName = currentRoster[attendeeId]?.attendeeName ?: ""
            return VideoCollectionTile(attendeeName, tileState)
        }

        return VideoCollectionTile("", tileState)
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) =
        notify("Audio started connecting. reconnecting: $reconnecting")

    override fun onAudioSessionStarted(reconnecting: Boolean) =
        notify("Audio successfully started. reconnecting: $reconnecting")

    override fun onAudioSessionDropped() {
        notify("Audio session dropped")
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        notify("Audio stopped for reason: ${sessionStatus.statusCode}")
        listener.onLeaveMeeting()
    }

    override fun onAudioSessionCancelledReconnect() = notify("Audio cancelled reconnecting")

    override fun onConnectionRecovered() = notify("Connection quality has recovered")

    override fun onConnectionBecamePoor() = notify("Connection quality has become poor")

    override fun onVideoSessionStartedConnecting() = notify("Video started connecting.")

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        if (sessionStatus.statusCode == MeetingSessionStatusCode.VideoAtCapacityViewOnly) {
            notify("Video encountered an error: ${sessionStatus.statusCode}")
        } else {
            notify("Video successfully started: ${sessionStatus.statusCode}")
        }
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) =
        notify("Video stopped for reason: ${sessionStatus.statusCode}")

    override fun onVideoTileAdded(tileState: VideoTileState) {
        uiScope.launch {
            logger.info(
                TAG,
                "Video track added, titleId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}" +
                        ", isContent ${tileState.isContent}"
            )
            if (tileState.isContent) {
                if (!currentScreenTiles.containsKey(tileState.tileId) && canShowMoreRemoteScreenTile()) {
                    showVideoTile(tileState)
                }
            } else {
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
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
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
            } else if (nextVideoTiles.containsKey(tileId)) {
                nextVideoTiles.remove(tileId)
            } else if (currentScreenTiles.containsKey(tileId)) {
                audioVideo.unbindVideoView(tileId)
                currentScreenTiles.remove(tileId)
                screenTileAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        if (tileState.pauseState == VideoPauseState.PausedForPoorConnection) {
            val attendeeName = currentRoster[tileState.attendeeId]?.attendeeName ?: ""
            notify(
                "Video for attendee $attendeeName " +
                        " has been paused for poor network connection," +
                        " video will automatically resume when connection improves"
            )
        }
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        val attendeeName = currentRoster[tileState.attendeeId]?.attendeeName ?: ""
        notify("Video for attendee $attendeeName has been unpaused")
    }

    override fun onMetricsReceived(metrics: Map<ObservableMetric, Any>) {
        logger.debug(TAG, "Media metrics received: $metrics")
    }

    private fun notify(message: String) {
        uiScope.launch {
            activity?.let {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
            logger.info(TAG, message)
        }
    }
}
