/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.adapter.MetricAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.RosterAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.VideoAdapter
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.model.MeetingModel
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MeetingFragment : Fragment(),
    RealtimeObserver, AudioVideoObserver, VideoTileObserver,
    MetricsObserver, ActiveSpeakerObserver, DeviceChangeObserver {
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val meetingModel: MeetingModel by lazy { ViewModelProvider(this)[MeetingModel::class.java] }

    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var listener: RosterViewEventListener
    override val scoreCallbackIntervalMs: Int? get() = 1000

    private val MAX_TILE_COUNT = 4
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

    private lateinit var buttonMute: ImageButton
    private lateinit var buttonCamera: ImageButton
    private lateinit var buttonVideo: ImageButton
    private lateinit var deviceAlertDialogBuilder: AlertDialog.Builder
    private lateinit var recyclerViewMetrics: RecyclerView
    private lateinit var recyclerViewRoster: RecyclerView
    private lateinit var recyclerViewVideoCollection: RecyclerView
    private lateinit var recyclerViewScreenShareCollection: RecyclerView
    private lateinit var metricsAdapter: MetricAdapter
    private lateinit var noVideoOrScreenShareAvailable: TextView
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoTileAdapter: VideoAdapter
    private lateinit var screenTileAdapter: VideoAdapter

    private var deviceListAdapter: ArrayAdapter<String>? = null

    companion object {
        fun newInstance(meetingId: String): MeetingFragment {
            val fragment = MeetingFragment()

            fragment.arguments =
                Bundle().apply { putString(HomeActivity.MEETING_ID_KEY, meetingId) }
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
        val view: View = inflater.inflate(R.layout.fragment_meeting, container, false)
        val activity = activity as Context

        audioVideo = (activity as MeetingActivity).getAudioVideo()

        view.findViewById<TextView>(R.id.textViewMeetingId)?.text = arguments?.getString(
            HomeActivity.MEETING_ID_KEY
        ) as String
        setupButtons(view)
        setupRecyclerViews(view)
        setupAlertDialog()

        noVideoOrScreenShareAvailable = view.findViewById(R.id.noVideoOrScreenShareAvailable)
        noVideoOrScreenShareAvailable.visibility = View.VISIBLE

        subscribeToAttendeeChangeHandlers()
        audioVideo.start()
        audioVideo.startRemoteVideo()
        return view
    }

    private fun setupButtons(view: View) {
        buttonMute = view.findViewById(R.id.buttonMute)
        buttonMute.setImageResource(if (meetingModel.isMuted) R.drawable.button_mute_on else R.drawable.button_mute)
        buttonMute.setOnClickListener { toggleMute() }

        buttonCamera = view.findViewById(R.id.buttonCamera)
        buttonCamera.setImageResource(if (meetingModel.isCameraOn) R.drawable.button_camera_on else R.drawable.button_camera)
        buttonCamera.setOnClickListener { toggleVideo() }

        buttonVideo = view.findViewById(R.id.buttonVideo)
        buttonVideo.setImageResource(if (meetingModel.isScreenShareViewOn) R.drawable.button_screen_share else R.drawable.button_attendees_video)
        buttonVideo.setOnClickListener { toggleScreenShare() }

        view.findViewById<ImageButton>(R.id.buttonMetrics)
            ?.setOnClickListener { toggleMetrics() }

        view.findViewById<ImageButton>(R.id.buttonSpeaker)
            ?.setOnClickListener { toggleSpeaker() }

        view.findViewById<ImageButton>(R.id.buttonAttendeesList)
            ?.setOnClickListener { toggleAttendeesList() }

        view.findViewById<ImageButton>(R.id.buttonLeave)
            ?.setOnClickListener { listener.onLeaveMeeting() }
    }

    private fun setupRecyclerViews(view: View) {
        recyclerViewMetrics = view.findViewById(R.id.recyclerViewMetrics)
        recyclerViewMetrics.layoutManager = LinearLayoutManager(activity)
        metricsAdapter = MetricAdapter(meetingModel.currentMetrics.values)
        recyclerViewMetrics.adapter = metricsAdapter
        recyclerViewMetrics.visibility = meetingModel.metricVisibility

        recyclerViewRoster = view.findViewById(R.id.recyclerViewRoster)
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        rosterAdapter = RosterAdapter(meetingModel.currentRoster.values)
        recyclerViewRoster.adapter = rosterAdapter
        recyclerViewRoster.visibility = meetingModel.rosterVisibility

        recyclerViewVideoCollection =
            view.findViewById(R.id.recyclerViewVideoCollection)
        recyclerViewVideoCollection.layoutManager = createLinearLayoutManagerForOrientation()
        videoTileAdapter = VideoAdapter(
            meetingModel.currentVideoTiles.values,
            audioVideo,
            context
        )
        recyclerViewVideoCollection.adapter = videoTileAdapter
        recyclerViewVideoCollection.visibility = meetingModel.videoVisibility

        recyclerViewScreenShareCollection =
            view.findViewById(R.id.recyclerViewScreenShareCollection)
        recyclerViewScreenShareCollection.layoutManager = LinearLayoutManager(activity)
        screenTileAdapter =
            VideoAdapter(
                meetingModel.currentScreenTiles.values,
                audioVideo,
                context
            )
        recyclerViewScreenShareCollection.adapter = screenTileAdapter
        recyclerViewScreenShareCollection.visibility = meetingModel.screenShareVisibility
    }

    private fun createLinearLayoutManagerForOrientation(): LinearLayoutManager {
        return if (isLandscapeMode(activity) == true) {
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(activity)
        }
    }

    private fun setupAlertDialog() {
        meetingModel.currentMediaDevices = audioVideo.listAudioDevices().filter { device -> device.type != MediaDeviceType.OTHER }
        deviceListAdapter =
            context?.let { ArrayAdapter(it, android.R.layout.simple_list_item_1, android.R.id.text1) }
        deviceListAdapter?.addAll(meetingModel.currentMediaDevices.map { device -> device.label })
        deviceAlertDialogBuilder = AlertDialog.Builder(activity)
        deviceAlertDialogBuilder.setTitle("Choose a device")
        deviceAlertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            meetingModel.isDeviceListDialogOn = false
        }
        deviceAlertDialogBuilder.setAdapter(deviceListAdapter) { _, which ->
            run {
                audioVideo.chooseAudioDevice(meetingModel.currentMediaDevices[which])
            }
        }
        deviceAlertDialogBuilder.setOnDismissListener {
            meetingModel.isDeviceListDialogOn = false
        }

        if (meetingModel.isDeviceListDialogOn) {
            deviceAlertDialogBuilder.create().show()
        }
    }

    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        meetingModel.currentMediaDevices = freshAudioDeviceList
            .filter { device -> device.type != MediaDeviceType.OTHER }
        val deviceNameList = meetingModel.currentMediaDevices.map { device -> device.label }
        deviceListAdapter?.clear()
        deviceListAdapter?.addAll(deviceNameList)
        deviceListAdapter?.notifyDataSetChanged()
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        uiScope.launch {
            mutex.withLock {
                volumeUpdates.forEach { (attendeeInfo, volumeLevel) ->
                    meetingModel.currentRoster[attendeeInfo.attendeeId]?.let {
                        meetingModel.currentRoster[attendeeInfo.attendeeId] =
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
                    meetingModel.currentRoster[attendeeInfo.attendeeId]?.let {
                        meetingModel.currentRoster[attendeeInfo.attendeeId] =
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
                    meetingModel.currentRoster.getOrPut(
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
                attendeeInfo.forEach { (attendeeId, _) ->
                    meetingModel.currentRoster.remove(
                        attendeeId
                    )
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (_, externalUserId) ->
            notify("$externalUserId dropped")
        }

        uiScope.launch {
            mutex.withLock {
                attendeeInfo.forEach { (attendeeId, _) ->
                    meetingModel.currentRoster.remove(
                        attendeeId
                    )
                }

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
                meetingModel.currentRoster.values.forEach { attendee ->
                    if (activeSpeakers.contains(attendee.attendeeId) != attendee.isActiveSpeaker) {
                        meetingModel.currentRoster[attendee.attendeeId] =
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

    private fun toggleMute() {
        if (meetingModel.isMuted) {
            audioVideo.realtimeLocalUnmute()
            buttonMute.setImageResource(R.drawable.button_mute)
        } else {
            audioVideo.realtimeLocalMute()
            buttonMute.setImageResource(R.drawable.button_mute_on)
        }
        meetingModel.isMuted = !meetingModel.isMuted
    }

    private fun toggleSpeaker() {
        deviceAlertDialogBuilder.create()
        deviceAlertDialogBuilder.show()
        meetingModel.isDeviceListDialogOn = true
    }

    private fun toggleAttendeesList() {
        recyclerViewMetrics.visibility = View.GONE
        meetingModel.metricVisibility = View.GONE

        if (recyclerViewRoster.visibility == View.VISIBLE) {
            recyclerViewRoster.visibility = View.GONE
            meetingModel.rosterVisibility = View.GONE
        } else {
            recyclerViewRoster.visibility = View.VISIBLE
            meetingModel.rosterVisibility = View.VISIBLE
        }
    }

    private fun toggleMetrics() {
        recyclerViewRoster.visibility = View.GONE
        meetingModel.rosterVisibility = View.GONE
        if (recyclerViewMetrics.visibility == View.VISIBLE) {
            recyclerViewMetrics.visibility = View.GONE
            meetingModel.metricVisibility = View.GONE
        } else {
            recyclerViewMetrics.visibility = View.VISIBLE
            meetingModel.metricVisibility = View.VISIBLE
        }
    }

    private fun toggleVideo() {
        if (meetingModel.isCameraOn) {
            audioVideo.stopLocalVideo()
            buttonCamera.setImageResource(R.drawable.button_camera)
        } else {
            if (hasPermissionsAlready()) {
                startLocalVideo()
            } else {
                requestPermissions(
                    WEBRTC_PERM,
                    WEBRTC_PERMISSION_REQUEST_CODE
                )
            }
        }
        meetingModel.isCameraOn = !meetingModel.isCameraOn
        refreshNoVideosOrScreenShareAvailableText()
    }

    private fun toggleScreenShare() {
        recyclerViewRoster.visibility = View.GONE
        meetingModel.rosterVisibility = View.GONE
        if (meetingModel.isScreenShareViewOn) {
            recyclerViewVideoCollection.visibility = View.VISIBLE
            meetingModel.videoVisibility = View.VISIBLE
            recyclerViewScreenShareCollection.visibility = View.GONE
            meetingModel.screenShareVisibility = View.GONE
            buttonVideo.setImageResource(R.drawable.button_attendees_video)
            noVideoOrScreenShareAvailable.text = getString(R.string.no_videos_available)
        } else {
            recyclerViewVideoCollection.visibility = View.GONE
            meetingModel.videoVisibility = View.GONE
            recyclerViewScreenShareCollection.visibility = View.VISIBLE
            meetingModel.screenShareVisibility = View.VISIBLE
            buttonVideo.setImageResource(R.drawable.button_screen_share)
            noVideoOrScreenShareAvailable.text = getString(R.string.no_screen_share_available)
        }
        refreshNoVideosOrScreenShareAvailableText()
        meetingModel.isScreenShareViewOn = !meetingModel.isScreenShareViewOn
        audioVideo.startRemoteVideo()
    }

    private fun refreshNoVideosOrScreenShareAvailableText() {
        if (recyclerViewVideoCollection.visibility == View.VISIBLE) {
            if (meetingModel.currentVideoTiles.size > 0) {
                noVideoOrScreenShareAvailable.visibility = View.GONE
            } else {
                noVideoOrScreenShareAvailable.visibility = View.VISIBLE
            }
        } else {
            if (meetingModel.currentScreenTiles.size > 0) {
                noVideoOrScreenShareAvailable.visibility = View.GONE
            } else {
                noVideoOrScreenShareAvailable.visibility = View.VISIBLE
            }
        }
    }

    private fun startLocalVideo() {
        audioVideo.startLocalVideo()
        buttonCamera.setImageResource(R.drawable.button_camera_on)
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

    private fun showVideoTile(tileState: VideoTileState) {
        if (tileState.isContent) {
            meetingModel.currentScreenTiles[tileState.tileId] =
                createVideoCollectionTile(tileState)
            screenTileAdapter.notifyDataSetChanged()
        } else {
            meetingModel.currentVideoTiles[tileState.tileId] =
                createVideoCollectionTile(tileState)
            videoTileAdapter.notifyDataSetChanged()
        }
    }

    private fun canShowMoreRemoteVideoTile(): Boolean {
        // Current max amount of tiles should preserve one spot for local video
        val currentMax =
            if (meetingModel.currentVideoTiles.containsKey(LOCAL_TILE_ID)) MAX_TILE_COUNT else MAX_TILE_COUNT - 1
        return meetingModel.currentVideoTiles.size < currentMax
    }

    private fun canShowMoreRemoteScreenTile(): Boolean {
        // only show 1 screen share tile
        return meetingModel.currentScreenTiles.isEmpty()
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        attendeeId?.let {
            val attendeeName = meetingModel.currentRoster[attendeeId]?.attendeeName ?: ""
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
        if (sessionStatus.statusCode != MeetingSessionStatusCode.OK) {
            listener.onLeaveMeeting()
        }
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
                if (!meetingModel.currentScreenTiles.containsKey(tileState.tileId) && canShowMoreRemoteScreenTile()) {
                    showVideoTile(tileState)
                }
            } else {
                // For local video, should show it anyway
                if (tileState.isLocalTile) {
                    showVideoTile(tileState)
                } else if (!meetingModel.currentVideoTiles.containsKey(tileState.tileId)) {
                    if (canShowMoreRemoteVideoTile()) {
                        showVideoTile(tileState)
                    } else {
                        meetingModel.nextVideoTiles[tileState.tileId] =
                            createVideoCollectionTile(tileState)
                    }
                }
            }
            refreshNoVideosOrScreenShareAvailableText()
        }
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        uiScope.launch {
            val tileId: Int = tileState.tileId

            logger.info(
                TAG,
                "Video track removed, titleId: $tileId, attendeeId: ${tileState.attendeeId}"
            )
            audioVideo.unbindVideoView(tileId)
            if (meetingModel.currentVideoTiles.containsKey(tileId)) {
                meetingModel.currentVideoTiles.remove(tileId)
                // Show next video tileState if available
                if (meetingModel.nextVideoTiles.isNotEmpty() && canShowMoreRemoteVideoTile()) {
                    val nextTileState: VideoTileState =
                        meetingModel.nextVideoTiles.entries.iterator().next()
                            .value.videoTileState
                    showVideoTile(nextTileState)
                    meetingModel.nextVideoTiles.remove(nextTileState.tileId)
                }
                videoTileAdapter.notifyDataSetChanged()
            } else if (meetingModel.nextVideoTiles.containsKey(tileId)) {
                meetingModel.nextVideoTiles.remove(tileId)
            } else if (meetingModel.currentScreenTiles.containsKey(tileId)) {
                meetingModel.currentScreenTiles.remove(tileId)
                screenTileAdapter.notifyDataSetChanged()
            }
            refreshNoVideosOrScreenShareAvailableText()
        }
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        if (tileState.pauseState == VideoPauseState.PausedForPoorConnection) {
            val attendeeName =
                meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
            notify(
                "Video for attendee $attendeeName " +
                        " has been paused for poor network connection," +
                        " video will automatically resume when connection improves"
            )
        }
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        val attendeeName = meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
        notify("Video for attendee $attendeeName has been unpaused")
    }

    override fun onMetricsReceived(metrics: Map<ObservableMetric, Any>) {
        logger.debug(TAG, "Media metrics received: $metrics")
        uiScope.launch {
            mutex.withLock {
                metrics.forEach { (metricsName, metricsValue) ->
                    if (metricsValue.toString() != null) {
                        meetingModel.currentMetrics[metricsName.name] =
                            MetricData(metricsName.name, metricsValue.toString())
                    }
                }
                metricsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun notify(message: String) {
        uiScope.launch {
            activity?.let {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
            logger.info(TAG, message)
        }
    }

    private fun subscribeToAttendeeChangeHandlers() {
        audioVideo.addAudioVideoObserver(this)
        audioVideo.addDeviceChangeObserver(this)
        audioVideo.addMetricsObserver(this)
        audioVideo.addRealtimeObserver(this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), this)
    }

    private fun unsubscribeFromAttendeeChangeHandlers() {
        audioVideo.removeAudioVideoObserver(this)
        audioVideo.removeDeviceChangeObserver(this)
        audioVideo.removeMetricsObserver(this)
        audioVideo.removeRealtimeObserver(this)
        audioVideo.removeVideoTileObserver(this)
        audioVideo.removeActiveSpeakerObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeFromAttendeeChangeHandlers()
    }
}
