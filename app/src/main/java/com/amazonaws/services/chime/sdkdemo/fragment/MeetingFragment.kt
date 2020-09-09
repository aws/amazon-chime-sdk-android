/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessage
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.MeetingService
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.adapter.DeviceAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MessageAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MetricAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.RosterAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.VideoAdapter
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.databinding.FragmentMeetingBinding
import com.amazonaws.services.chime.sdkdemo.model.MeetingModel
import com.amazonaws.services.chime.sdkdemo.utils.AttendeeUtils
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MeetingFragment : Fragment(),
    RealtimeObserver, AudioVideoObserver, VideoTileObserver,
    MetricsObserver, ActiveSpeakerObserver, DeviceChangeObserver, DataMessageObserver {
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val meetingModel: MeetingModel by lazy { ViewModelProvider(this)[MeetingModel::class.java] }

    private lateinit var credentials: MeetingSessionCredentials
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var listener: RosterViewEventListener
    private lateinit var roster: ConcurrentHashMap<String, RosterAttendee>
    override val scoreCallbackIntervalMs: Int? get() = 1000

    private val MAX_TILE_COUNT = 4
    private val LOCAL_TILE_ID = 0
    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MeetingFragment"
    private lateinit var mService: MeetingService
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeetingService.MeetingBinder
            mService = binder.getService()
            mBound = true
            if (mService.audioVideo != null && mService.credentials != null) {
                audioVideo = mService.audioVideo as AudioVideoFacade
                credentials = mService.credentials as MeetingSessionCredentials
                roster = mService.attendees
                setupButtonsBar()
                setupSubViews()
                setupTab()
                setupAudioDeviceSelectionDialog()
                selectTab(meetingModel.tabIndex)
                subscribeToAttendeeChangeHandlers()
            }
        }
    }
    // Check if attendee Id contains this at the end to identify content share

    // Append to attendee name if it's for content share
    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.CAMERA
    )
    private val DATA_MESSAGE_TOPIC = "chat"
    private val DATA_MESSAGE_LIFETIME_MS = 300000

    enum class SubTab(val position: Int) {
        Attendees(0),
        Chat(1),
        Video(2),
        Screen(3),
        Metrics(4)
    }

    private lateinit var noVideoOrScreenShareAvailable: TextView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonMute: ImageButton
    private lateinit var buttonCamera: ImageButton
    private lateinit var deviceAlertDialogBuilder: AlertDialog.Builder
    private lateinit var viewChat: LinearLayout
    private lateinit var recyclerViewMetrics: RecyclerView
    private lateinit var recyclerViewRoster: RecyclerView
    private lateinit var recyclerViewVideoCollection: RecyclerView
    private lateinit var recyclerViewScreenShareCollection: RecyclerView
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var deviceListAdapter: DeviceAdapter
    private lateinit var metricsAdapter: MetricAdapter
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoTileAdapter: VideoAdapter
    private lateinit var screenTileAdapter: VideoAdapter
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var binding: FragmentMeetingBinding

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
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_meeting, container, false)

        binding.textViewMeetingId.text = arguments?.getString(
            HomeActivity.MEETING_ID_KEY
        ) as String
        noVideoOrScreenShareAvailable = binding.noVideoOrScreenShareAvailable
        refreshNoVideosOrScreenShareAvailableText()
        return binding.root
    }

    private fun setupButtonsBar() {
        buttonMute = binding.buttonMute
        buttonMute.setImageResource(if (meetingModel.isMuted) R.drawable.button_mute_on else R.drawable.button_mute)
        buttonMute.setOnClickListener { toggleMute() }

        buttonCamera = binding.buttonCamera
        buttonCamera.setImageResource(if (meetingModel.isCameraOn) R.drawable.button_camera_on else R.drawable.button_camera)
        buttonCamera.setOnClickListener { toggleVideo() }

        binding.buttonSpeaker.setOnClickListener { toggleSpeaker() }
        binding.buttonLeave.setOnClickListener {
            if (mBound) {
                stopAudioVideoAndUnbindService()
                MeetingService.stopService(requireContext())
                mBound = false
            }
            listener.onLeaveMeeting()
        }
    }

    private fun setupSubViews() {
        // Roster
        recyclerViewRoster = binding.recyclerViewRoster
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        rosterAdapter = RosterAdapter(roster.values)
        recyclerViewRoster.adapter = rosterAdapter
        recyclerViewRoster.visibility = View.VISIBLE

        // Video (camera & content)
        recyclerViewVideoCollection = binding.recyclerViewVideoCollection
        recyclerViewVideoCollection.layoutManager = createLinearLayoutManagerForOrientation()
        videoTileAdapter = VideoAdapter(
            meetingModel.currentVideoTiles.values,
            audioVideo,
            context
        )
        recyclerViewVideoCollection.adapter = videoTileAdapter
        recyclerViewVideoCollection.visibility = View.GONE

        recyclerViewScreenShareCollection =
            binding.recyclerViewScreenShareCollection
        recyclerViewScreenShareCollection.layoutManager = LinearLayoutManager(activity)
        screenTileAdapter =
            VideoAdapter(
                meetingModel.currentScreenTiles.values,
                audioVideo,
                context
            )
        recyclerViewScreenShareCollection.adapter = screenTileAdapter
        recyclerViewScreenShareCollection.visibility = View.GONE

        recyclerViewMetrics = binding.recyclerViewMetrics
        recyclerViewMetrics.layoutManager = LinearLayoutManager(activity)
        metricsAdapter = MetricAdapter(meetingModel.currentMetrics.values)
        recyclerViewMetrics.adapter = metricsAdapter
        recyclerViewMetrics.visibility = View.GONE

        // Chat
        viewChat = binding.subViewChat
        recyclerViewMessages = binding.recyclerViewMessages
        recyclerViewMessages.layoutManager = LinearLayoutManager(activity)
        messageAdapter = MessageAdapter(meetingModel.currentMessages)
        recyclerViewMessages.adapter = messageAdapter

        editTextMessage = binding.editTextChatBox
        editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                    true
                }
                else -> false
            }
        }
        binding.buttonSubmitMessage.let {
            it.setOnClickListener { sendMessage() }
        }

        viewChat.visibility = View.GONE
    }

    private fun setupTab() {
        tabLayout = binding.tabLayoutMeetingView
        SubTab.values().forEach {
            tabLayout.addTab(
                tabLayout.newTab().setText(it.name).setContentDescription("${it.name} Tab")
            )
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == SubTab.Video.position || tab?.position == SubTab.Screen.position) audioVideo.startRemoteVideo()
                showViewAt(tab?.position ?: SubTab.Attendees.position)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab?.position == SubTab.Video.position || tab?.position == SubTab.Screen.position)
                    audioVideo.stopRemoteVideo()
            }
        })
    }

    private fun showViewAt(index: Int) {
        recyclerViewRoster.visibility = View.GONE
        viewChat.visibility = View.GONE
        recyclerViewVideoCollection.visibility = View.GONE
        recyclerViewScreenShareCollection.visibility = View.GONE
        recyclerViewMetrics.visibility = View.GONE

        when (index) {
            SubTab.Attendees.position -> {
                recyclerViewRoster.visibility = View.VISIBLE
            }
            SubTab.Chat.position -> {
                viewChat.visibility = View.VISIBLE
                scrollToLastMessage()
            }
            SubTab.Video.position -> {
                recyclerViewVideoCollection.visibility = View.VISIBLE
            }
            SubTab.Screen.position -> {
                recyclerViewScreenShareCollection.visibility = View.VISIBLE
            }
            SubTab.Metrics.position -> {
                recyclerViewMetrics.visibility = View.VISIBLE
            }
            else -> return
        }
        meetingModel.tabIndex = index
        refreshNoVideosOrScreenShareAvailableText()
    }

    private fun selectTab(index: Int) {
        tabLayout.selectTab(tabLayout.getTabAt(index))
    }

    private fun createLinearLayoutManagerForOrientation(): LinearLayoutManager {
        return if (isLandscapeMode(activity) == true) {
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(activity)
        }
    }

    private fun setupAudioDeviceSelectionDialog() {
        meetingModel.currentMediaDevices =
            audioVideo.listAudioDevices().filter { device -> device.type != MediaDeviceType.OTHER }
        deviceListAdapter = DeviceAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            meetingModel.currentMediaDevices
        )
        deviceAlertDialogBuilder = AlertDialog.Builder(activity)
        deviceAlertDialogBuilder.setTitle(R.string.alert_title_choose_audio)
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
        deviceListAdapter.clear()
        deviceListAdapter.addAll(meetingModel.currentMediaDevices)
        deviceListAdapter.notifyDataSetChanged()
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        uiScope.launch {
            volumeUpdates.forEach { (attendeeInfo, volumeLevel) ->
                roster[attendeeInfo.attendeeId]?.let {
                    roster[attendeeInfo.attendeeId] =
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

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        uiScope.launch {
            signalUpdates.forEach { (attendeeInfo, signalStrength) ->
                logWithFunctionName(
                    "onSignalStrengthChanged",
                    "${attendeeInfo.externalUserId} $signalStrength",
                    LogLevel.DEBUG
                )
                roster[attendeeInfo.attendeeId]?.let {
                    roster[attendeeInfo.attendeeId] =
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

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            mService.onAttendeesJoined(attendeeInfo)
            rosterAdapter.notifyDataSetChanged()
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            mService.onAttendeesRemoved(attendeeInfo)
            rosterAdapter.notifyDataSetChanged()
        }
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (_, externalUserId) ->
            notifyHandler("$externalUserId dropped")
            logWithFunctionName(
                object {}.javaClass.enclosingMethod?.name,
                "$externalUserId dropped"
            )
        }

        uiScope.launch {
            mService.onAttendeesRemoved(attendeeInfo)
            rosterAdapter.notifyDataSetChanged()
        }
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            logWithFunctionName(
                object {}.javaClass.enclosingMethod?.name,
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
                roster.values.forEach { attendee ->
                    if (activeSpeakers.contains(attendee.attendeeId) != attendee.isActiveSpeaker) {
                        roster[attendee.attendeeId] =
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
        val scoresStr =
            scores.map { entry -> "${entry.key.externalUserId}: ${entry.value}" }.joinToString(",")
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            scoresStr,
            LogLevel.DEBUG
        )
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

    private fun toggleVideo() {
        if (meetingModel.isCameraOn) {
            audioVideo.stopLocalVideo()
            buttonCamera.setImageResource(R.drawable.button_camera)
        } else {
            if (hasPermissionsAlready()) {
                startLocalVideo()
                logWithFunctionName("getActiveCamera", "${audioVideo.getActiveCamera()?.type}")
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

    private fun refreshNoVideosOrScreenShareAvailableText() {
        if (meetingModel.tabIndex == SubTab.Video.position) {
            if (meetingModel.currentVideoTiles.isNotEmpty()) {
                noVideoOrScreenShareAvailable.visibility = View.GONE
            } else {
                noVideoOrScreenShareAvailable.text = getString(R.string.no_videos_available)
                noVideoOrScreenShareAvailable.visibility = View.VISIBLE
            }
        } else if (meetingModel.tabIndex == SubTab.Screen.position) {
            if (meetingModel.currentScreenTiles.isNotEmpty()) {
                noVideoOrScreenShareAvailable.visibility = View.GONE
            } else {
                noVideoOrScreenShareAvailable.text = getString(R.string.no_screen_share_available)
                noVideoOrScreenShareAvailable.visibility = View.VISIBLE
            }
        } else {
            noVideoOrScreenShareAvailable.visibility = View.GONE
        }
    }

    private fun startLocalVideo() {
        audioVideo.startLocalVideo()
        buttonCamera.setImageResource(R.drawable.button_camera_on)
        selectTab(SubTab.Video.position)
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
        val attendeeName = roster[attendeeId]?.attendeeName ?: ""
        return VideoCollectionTile(attendeeName, tileState)
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        notifyHandler(
            "Audio started connecting. reconnecting: $reconnecting"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "reconnecting: $reconnecting"
        )
    }

    override fun onAudioSessionStarted(reconnecting: Boolean) {
        notifyHandler(
            "Audio successfully started. reconnecting: $reconnecting"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "reconnecting: $reconnecting"
        )
    }

    override fun onAudioSessionDropped() {
        notifyHandler("Audio session dropped")
        logWithFunctionName(object {}.javaClass.enclosingMethod?.name)
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        notifyHandler(
            "Audio stopped for reason: ${sessionStatus.statusCode}"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "${sessionStatus.statusCode}"
        )
        if (sessionStatus.statusCode != MeetingSessionStatusCode.OK) {
            listener.onLeaveMeeting()
        }
    }

    override fun onAudioSessionCancelledReconnect() {
        notifyHandler("Audio cancelled reconnecting")
        logWithFunctionName(object {}.javaClass.enclosingMethod?.name)
    }

    override fun onConnectionRecovered() {
        notifyHandler(
            "Connection quality has recovered"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name
        )
    }

    override fun onConnectionBecamePoor() {
        notifyHandler(
            "Connection quality has become poor"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name
        )
    }

    override fun onVideoSessionStartedConnecting() {
        notifyHandler("Video started connecting.")
        logWithFunctionName(object {}.javaClass.enclosingMethod?.name)
    }

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        val message =
            if (sessionStatus.statusCode == MeetingSessionStatusCode.VideoAtCapacityViewOnly) "Video encountered an error: ${sessionStatus.statusCode}" else "Video successfully started: ${sessionStatus.statusCode}"

        notifyHandler(message)
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "${sessionStatus.statusCode}"
        )
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        notifyHandler(
            "Video stopped for reason: ${sessionStatus.statusCode}"
        )
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "${sessionStatus.statusCode}"
        )
    }

    override fun onVideoTileAdded(tileState: VideoTileState) {
        uiScope.launch {
            logger.info(
                TAG,
                "Video track added, titleId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}" +
                        ", isContent ${tileState.isContent} with size ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight}"
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
                roster[tileState.attendeeId]?.attendeeName ?: ""
            notifyHandler(
                "Video for attendee $attendeeName " +
                        " has been paused for poor network connection," +
                        " video will automatically resume when connection improves"
            )
            logWithFunctionName(
                object {}.javaClass.enclosingMethod?.name,
                "$attendeeName video paused"
            )
        }
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        val attendeeName = roster[tileState.attendeeId]?.attendeeName ?: ""
        notifyHandler("Video for attendee $attendeeName has been unpaused")
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "$attendeeName video resumed"
        )
    }

    override fun onVideoTileSizeChanged(tileState: VideoTileState) {
        logger.info(
            TAG,
            "Video stream content size changed to ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight} for tileId: ${tileState.tileId}"
        )
    }

    override fun onMetricsReceived(metrics: Map<ObservableMetric, Any>) {
        logger.debug(TAG, "Media metrics received: $metrics")
        uiScope.launch {
            mutex.withLock {
                metrics.forEach { (metricsName, metricsValue) ->
                    meetingModel.currentMetrics[metricsName.name] =
                        MetricData(metricsName.name, metricsValue.toString())
                }
                metricsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun sendMessage() {
        val text = editTextMessage.text.toString().trim()
        if (text.isBlank()) return
        audioVideo.realtimeSendDataMessage(
            DATA_MESSAGE_TOPIC,
            text,
            DATA_MESSAGE_LIFETIME_MS
        )
        editTextMessage.text.clear()
        // echo the message to the handler
        onDataMessageReceived(
            DataMessage(
                Calendar.getInstance().timeInMillis,
                DATA_MESSAGE_TOPIC,
                text.toByteArray(),
                credentials.attendeeId,
                credentials.externalUserId,
                false
            )
        )
    }

    override fun onDataMessageReceived(dataMessage: DataMessage) {
        if (!dataMessage.throttled) {
            if (dataMessage.timestampMs <= meetingModel.lastReceivedMessageTimestamp) return
            meetingModel.lastReceivedMessageTimestamp = dataMessage.timestampMs
            meetingModel.currentMessages.add(
                Message(
                    AttendeeUtils.getAttendeeName(
                        dataMessage.senderAttendeeId,
                        dataMessage.senderExternalUserId
                    ),
                    dataMessage.timestampMs,
                    dataMessage.text(),
                    dataMessage.senderAttendeeId == credentials.attendeeId
                )
            )
            messageAdapter.notifyItemInserted(meetingModel.currentMessages.size - 1)
            scrollToLastMessage()
        } else {
            notifyHandler("Message is throttled. Please resend")
        }
    }

    private fun scrollToLastMessage() {
        if (meetingModel.currentMessages.isNotEmpty()) {
            recyclerViewMessages.scrollToPosition(meetingModel.currentMessages.size - 1)
        }
    }

    private fun notifyHandler(
        toastMessage: String
    ) {
        uiScope.launch {
            activity?.let {
                Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logWithFunctionName(
        fnName: String?,
        msg: String = "",
        logLevel: LogLevel = LogLevel.INFO
    ) {
        val newMsg = if (fnName == null) msg else "[Function] [$fnName]: $msg"
        when (logLevel) {
            LogLevel.DEBUG -> logger.debug(TAG, newMsg)
            else -> logger.info(TAG, newMsg)
        }
    }

    private fun subscribeToAttendeeChangeHandlers() {
        audioVideo.addAudioVideoObserver(this)
        audioVideo.addDeviceChangeObserver(this)
        audioVideo.addMetricsObserver(this)
        audioVideo.addRealtimeObserver(this)
        audioVideo.addRealtimeDataMessageObserver(DATA_MESSAGE_TOPIC, this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), this)
    }

    private fun unsubscribeFromAttendeeChangeHandlers() {
        audioVideo.removeAudioVideoObserver(this)
        audioVideo.removeDeviceChangeObserver(this)
        audioVideo.removeMetricsObserver(this)
        audioVideo.removeRealtimeObserver(this)
        audioVideo.removeRealtimeDataMessageObserverFromTopic(DATA_MESSAGE_TOPIC)
        audioVideo.removeVideoTileObserver(this)
        audioVideo.removeActiveSpeakerObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            unsubscribeFromAttendeeChangeHandlers()
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(context, MeetingService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            stopAudioVideoAndUnbindService()
            mBound = false
        }
    }

    private fun stopAudioVideoAndUnbindService() {
        mService.audioVideo?.stopRemoteVideo()
        mService.audioVideo?.stopLocalVideo()
        activity?.unbindService(connection)
    }
}
