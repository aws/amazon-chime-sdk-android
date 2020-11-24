/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
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
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.adapter.DeviceAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MessageAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MetricAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.RosterAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.VideoAdapter
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.device.AudioDeviceManager
import com.amazonaws.services.chime.sdkdemo.model.MeetingModel
import com.amazonaws.services.chime.sdkdemo.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdkdemo.utils.GpuVideoProcessor
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
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

    private var deviceDialog: AlertDialog? = null

    private lateinit var credentials: MeetingSessionCredentials
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private lateinit var gpuVideoProcessor: GpuVideoProcessor
    private lateinit var cpuVideoProcessor: CpuVideoProcessor
    private lateinit var listener: RosterViewEventListener
    override val scoreCallbackIntervalMs: Int? get() = 1000

    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MeetingFragment"

    // Check if attendee Id contains this at the end to identify content share
    private val CONTENT_DELIMITER = "#content"

    // Append to attendee name if it's for content share
    private val CONTENT_NAME_SUFFIX = "<<Content>>"

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
    private lateinit var additionalOptionsAlertDialogBuilder: AlertDialog.Builder
    private lateinit var viewChat: LinearLayout
    private lateinit var recyclerViewMetrics: RecyclerView
    private lateinit var recyclerViewRoster: RecyclerView
    private lateinit var viewVideo: LinearLayout
    private lateinit var recyclerViewVideoCollection: RecyclerView
    private lateinit var prevVideoPageButton: Button
    private lateinit var nextVideoPageButton: Button
    private lateinit var recyclerViewScreenShareCollection: RecyclerView
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var deviceListAdapter: DeviceAdapter
    private lateinit var metricsAdapter: MetricAdapter
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoTileAdapter: VideoAdapter
    private lateinit var screenTileAdapter: VideoAdapter
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var audioDeviceManager: AudioDeviceManager

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

        credentials = (activity as MeetingActivity).getMeetingSessionCredentials()
        audioVideo = activity.getAudioVideo()
        cameraCaptureSource = activity.getCameraCaptureSource()
        gpuVideoProcessor = activity.getGpuVideoProcessor()
        cpuVideoProcessor = activity.getCpuVideoProcessor()
        audioDeviceManager = AudioDeviceManager(audioVideo)

        view.findViewById<TextView>(R.id.textViewMeetingId)?.text = arguments?.getString(
            HomeActivity.MEETING_ID_KEY
        ) as String
        setupButtonsBar(view)
        setupSubViews(view)
        setupTab(view)
        setupAudioDeviceSelectionDialog()
        setupAdditionalOptionsDialog()

        noVideoOrScreenShareAvailable = view.findViewById(R.id.noVideoOrScreenShareAvailable)
        refreshNoVideosOrScreenShareAvailableText()

        selectTab(meetingModel.tabIndex)
        subscribeToAttendeeChangeHandlers()
        audioVideo.start()
        audioVideo.startRemoteVideo()
        return view
    }

    private fun setupButtonsBar(view: View) {
        buttonMute = view.findViewById(R.id.buttonMute)
        buttonMute.setImageResource(if (meetingModel.isMuted) R.drawable.button_mute_on else R.drawable.button_mute)
        buttonMute.setOnClickListener { toggleMute() }

        buttonCamera = view.findViewById(R.id.buttonCamera)
        buttonCamera.setImageResource(if (meetingModel.isCameraOn) R.drawable.button_camera_on else R.drawable.button_camera)
        buttonCamera.setOnClickListener { toggleVideo() }

        view.findViewById<ImageButton>(R.id.buttonMore)
            ?.setOnClickListener { toggleAdditionalOptionsMenu() }

        view.findViewById<ImageButton>(R.id.buttonSpeaker)
            ?.setOnClickListener { toggleSpeaker() }

        view.findViewById<ImageButton>(R.id.buttonLeave)
            ?.setOnClickListener { endMeeting() }
    }

    private fun setupSubViews(view: View) {
        // Roster
        recyclerViewRoster = view.findViewById(R.id.recyclerViewRoster)
        recyclerViewRoster.layoutManager = LinearLayoutManager(activity)
        rosterAdapter = RosterAdapter(meetingModel.currentRoster.values)
        recyclerViewRoster.adapter = rosterAdapter
        recyclerViewRoster.visibility = View.VISIBLE

        // Video (camera & content)
        viewVideo = view.findViewById(R.id.subViewVideo)
        viewVideo.visibility = View.GONE

        prevVideoPageButton = view.findViewById(R.id.prevVideoPageButton)
        prevVideoPageButton.setOnClickListener {
            if (meetingModel.canGoToPrevVideoPage()) {
                meetingModel.currentVideoPageIndex -= 1
                onVideoPageUpdated()
            }
        }

        nextVideoPageButton = view.findViewById(R.id.nextVideoPageButton)
        nextVideoPageButton.setOnClickListener {
            if (meetingModel.canGoToNextVideoPage()) {
                meetingModel.currentVideoPageIndex += 1
                onVideoPageUpdated()
            }
        }

        recyclerViewVideoCollection =
            view.findViewById(R.id.recyclerViewVideoCollection)
        recyclerViewVideoCollection.layoutManager = createLinearLayoutManagerForOrientation()
        videoTileAdapter = VideoAdapter(
            meetingModel.videoStatesInCurrentPage,
            meetingModel.userPausedVideoTileIds,
            audioVideo,
            cameraCaptureSource,
            context,
            logger
        )
        recyclerViewVideoCollection.adapter = videoTileAdapter

        recyclerViewScreenShareCollection =
            view.findViewById(R.id.recyclerViewScreenShareCollection)
        recyclerViewScreenShareCollection.layoutManager = LinearLayoutManager(activity)
        screenTileAdapter =
            VideoAdapter(
                meetingModel.currentScreenTiles,
                meetingModel.userPausedVideoTileIds,
                audioVideo,
                null,
                context,
                logger
            )
        recyclerViewScreenShareCollection.adapter = screenTileAdapter
        recyclerViewScreenShareCollection.visibility = View.GONE

        recyclerViewMetrics = view.findViewById(R.id.recyclerViewMetrics)
        recyclerViewMetrics.layoutManager = LinearLayoutManager(activity)
        metricsAdapter = MetricAdapter(meetingModel.currentMetrics.values)
        recyclerViewMetrics.adapter = metricsAdapter
        recyclerViewMetrics.visibility = View.GONE

        // Chat
        viewChat = view.findViewById(R.id.subViewChat)
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages)
        recyclerViewMessages.layoutManager = LinearLayoutManager(activity)
        messageAdapter = MessageAdapter(meetingModel.currentMessages)
        recyclerViewMessages.adapter = messageAdapter

        editTextMessage = view.findViewById(R.id.editTextChatBox)
        editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                    true
                }
                else -> false
            }
        }
        view.findViewById<ImageButton>(R.id.buttonSubmitMessage)?.let {
            it.setOnClickListener { sendMessage() }
        }

        viewChat.visibility = View.GONE
    }

    private fun setupTab(view: View) {
        tabLayout = view.findViewById(R.id.tabLayoutMeetingView)
        SubTab.values().forEach {
            tabLayout.addTab(
                tabLayout.newTab().setText(it.name).setContentDescription("${it.name} Tab")
            )
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showViewAt(tab?.position ?: SubTab.Attendees.position)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab?.position == SubTab.Video.position) {
                    pauseAllRemoteVideos()
                    setVideoSurfaceViewsVisibility(View.GONE)
                } else if (tab?.position == SubTab.Screen.position) {
                    pauseAllContentShares()
                    setScreenSurfaceViewsVisibility(View.GONE)
                }
            }
        })
    }

    private fun setVideoSurfaceViewsVisibility(visibility: Int) {
        meetingModel.localVideoTileState?.videoRenderView?.visibility = visibility
        meetingModel.remoteVideoTileStates.forEach { it.videoRenderView?.visibility = visibility }
    }

    private fun setScreenSurfaceViewsVisibility(visibility: Int) {
        meetingModel.currentScreenTiles.forEach { it.videoRenderView?.visibility = visibility }
    }

    private fun showViewAt(index: Int) {
        recyclerViewRoster.visibility = View.GONE
        viewChat.visibility = View.GONE
        viewVideo.visibility = View.GONE
        recyclerViewScreenShareCollection.visibility = View.GONE
        recyclerViewMetrics.visibility = View.GONE

        meetingModel.tabIndex = index
        when (index) {
            SubTab.Attendees.position -> {
                recyclerViewRoster.visibility = View.VISIBLE
            }
            SubTab.Chat.position -> {
                viewChat.visibility = View.VISIBLE
                scrollToLastMessage()
            }
            SubTab.Video.position -> {
                viewVideo.visibility = View.VISIBLE
                setVideoSurfaceViewsVisibility(View.VISIBLE)
                onVideoPageUpdated()
            }
            SubTab.Screen.position -> {
                recyclerViewScreenShareCollection.visibility = View.VISIBLE
                setScreenSurfaceViewsVisibility(View.VISIBLE)
                resumeAllContentSharesExceptUserPausedVideos()
            }
            SubTab.Metrics.position -> {
                recyclerViewMetrics.visibility = View.VISIBLE
            }
            else -> return
        }
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
            meetingModel.currentMediaDevices,
            audioVideo,
            audioDeviceManager)
        deviceAlertDialogBuilder = AlertDialog.Builder(activity)
        deviceAlertDialogBuilder.setTitle(R.string.alert_title_choose_audio)
        deviceAlertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            meetingModel.isDeviceListDialogOn = false
        }
        deviceAlertDialogBuilder.setAdapter(deviceListAdapter) { _, which ->
            run {
                audioVideo.chooseAudioDevice(meetingModel.currentMediaDevices[which])
                audioDeviceManager.setActiveAudioDevice(meetingModel.currentMediaDevices[which])
                meetingModel.isDeviceListDialogOn = false
            }
        }

        deviceAlertDialogBuilder.setOnCancelListener {
            meetingModel.isDeviceListDialogOn = false
        }
        if (meetingModel.isDeviceListDialogOn) {
            deviceDialog = deviceAlertDialogBuilder.create()
            deviceDialog?.show()
        }
    }

    private fun setupAdditionalOptionsDialog() {
        additionalOptionsAlertDialogBuilder = AlertDialog.Builder(activity)
        additionalOptionsAlertDialogBuilder.setTitle(R.string.alert_title_additional_options)
        additionalOptionsAlertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            meetingModel.isAdditionalOptionsDialogOn = false
        }
        refreshAdditionalOptionsDialogItems()
        additionalOptionsAlertDialogBuilder.setOnDismissListener {
            meetingModel.isAdditionalOptionsDialogOn = false
        }

        if (meetingModel.isAdditionalOptionsDialogOn) {
            additionalOptionsAlertDialogBuilder.create().show()
        }
    }

    private fun refreshAdditionalOptionsDialogItems() {
        val isVoiceFocusEnabled = audioVideo.realtimeIsVoiceFocusEnabled()

        val additionalToggles = arrayOf(
            context?.getString(if (isVoiceFocusEnabled) R.string.disable_voice_focus else R.string.enable_voice_focus),
            context?.getString(R.string.toggle_flashlight),
            context?.getString(R.string.toggle_cpu_filter),
            context?.getString(R.string.toggle_gpu_filter),
            context?.getString(R.string.toggle_custom_capture_source)
        )

        additionalOptionsAlertDialogBuilder.setItems(additionalToggles) { _, which ->
            when (which) {
                0 -> setVoiceFocusEnabled(!isVoiceFocusEnabled)
                1 -> toggleFlashlight()
                2 -> toggleCpuDemoFilter()
                3 -> toggleGpuDemoFilter()
                4 -> toggleCustomCaptureSource()
            }
        }
    }

    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        meetingModel.currentMediaDevices = freshAudioDeviceList
            .filter { device -> device.type != MediaDeviceType.OTHER }
        audioDeviceManager.reconfigureActiveAudioDevice()
        deviceListAdapter.clear()
        deviceListAdapter.addAll(meetingModel.currentMediaDevices)
        deviceListAdapter.notifyDataSetChanged()
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
                    logWithFunctionName(
                        "onSignalStrengthChanged",
                        "${attendeeInfo.externalUserId} $signalStrength",
                        LogLevel.DEBUG
                    )
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
            notifyHandler("$externalUserId dropped")
            logWithFunctionName(
                object {}.javaClass.enclosingMethod?.name,
                "$externalUserId dropped"
            )
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
                    meetingModel.updateRemoteVideoStatesBasedOnActiveSpeakers(attendeeInfo)
                    onVideoPageUpdated()

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
        meetingModel.currentMediaDevices = audioVideo.listAudioDevices().filter { it.type != MediaDeviceType.OTHER }
        deviceListAdapter.clear()
        deviceListAdapter.addAll(meetingModel.currentMediaDevices)
        deviceListAdapter.notifyDataSetChanged()
        deviceDialog = deviceAlertDialogBuilder.create()
        deviceDialog?.show()
        meetingModel.isDeviceListDialogOn = true
    }

    private fun setVoiceFocusEnabled(enabled: Boolean) {
        val action = if (enabled) "enable" else "disable"

        val success = audioVideo.realtimeSetVoiceFocusEnabled(enabled)

        if (success) {
            notifyHandler("Voice Focus ${action}d")
            refreshAdditionalOptionsDialogItems()
        } else {
            notifyHandler("Failed to $action Voice Focus")
        }
    }

    private fun toggleVideo() {
        if (meetingModel.isCameraOn) {
            stopLocalVideo()
        } else {
            startLocalVideo()
        }
        meetingModel.isCameraOn = !meetingModel.isCameraOn
        refreshNoVideosOrScreenShareAvailableText()
    }

    private fun toggleAdditionalOptionsMenu() {
        additionalOptionsAlertDialogBuilder.create()
        additionalOptionsAlertDialogBuilder.show()
        meetingModel.isAdditionalOptionsDialogOn = true
    }

    private fun toggleFlashlight() {
        logger.info(
            TAG,
            "Toggling flashlight from ${cameraCaptureSource.torchEnabled} to ${!cameraCaptureSource.torchEnabled}"
        )
        if (!meetingModel.isUsingCameraCaptureSource) {
            logger.warn(TAG, "Cannot toggle flashlight without using custom camera capture source")
            Toast.makeText(
                context,
                getString(R.string.user_notification_flashlight_custom_source_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val desiredFlashlightEnabled = !cameraCaptureSource.torchEnabled
        cameraCaptureSource.torchEnabled = desiredFlashlightEnabled
        if (cameraCaptureSource.torchEnabled != desiredFlashlightEnabled) {
            logger.warn(TAG, "Flashlight failed to toggle")
            Toast.makeText(
                context,
                getString(R.string.user_notification_flashlight_unavailable_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    private fun toggleCpuDemoFilter() {
        if (!meetingModel.isUsingCameraCaptureSource) {
            logger.warn(TAG, "Cannot toggle filter without using custom camera capture source")
            Toast.makeText(
                context,
                getString(R.string.user_notification_filter_custom_source_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (meetingModel.isUsingGpuVideoProcessor) {
            logger.warn(TAG, "Cannot toggle filter when other filter is enabled")
            Toast.makeText(
                context,
                getString(R.string.user_notification_filter_both_enabled_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        logger.info(
            TAG,
            "Toggling CPU demo filter from $meetingModel.isUsingCpuVideoProcessor to ${!meetingModel.isUsingCpuVideoProcessor}"
        )
        meetingModel.isUsingCpuVideoProcessor = !meetingModel.isUsingCpuVideoProcessor
        if (meetingModel.isLocalVideoStarted) {
            startLocalVideo()
        }
    }

    private fun toggleGpuDemoFilter() {
        if (!meetingModel.isUsingCameraCaptureSource) {
            logger.warn(TAG, "Cannot toggle filter without using custom camera capture source")
            Toast.makeText(
                context,
                getString(R.string.user_notification_filter_custom_source_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (meetingModel.isUsingCpuVideoProcessor) {
            logger.warn(TAG, "Cannot toggle filter when other filter is enabled")
            Toast.makeText(
                context,
                getString(R.string.user_notification_filter_both_enabled_error),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        logger.info(
            TAG,
            "Toggling GPU demo filter from $meetingModel.isUsingGpuVideoProcessor to ${!meetingModel.isUsingGpuVideoProcessor}"
        )
        meetingModel.isUsingGpuVideoProcessor = !meetingModel.isUsingGpuVideoProcessor
        if (meetingModel.isLocalVideoStarted) {
            startLocalVideo()
        }
    }

    private fun toggleCustomCaptureSource() {
        logger.info(
            TAG,
            "Toggling using custom camera source from $meetingModel.isUsingCameraCaptureSource to ${!meetingModel.isUsingCameraCaptureSource}"
        )
        val wasUsingCameraCaptureSource = meetingModel.isUsingCameraCaptureSource
        meetingModel.isUsingCameraCaptureSource = !meetingModel.isUsingCameraCaptureSource
        if (meetingModel.isLocalVideoStarted) {
            if (wasUsingCameraCaptureSource) {
                cameraCaptureSource.stop()
            }
            startLocalVideo()
        }
    }

    private fun refreshNoVideosOrScreenShareAvailableText() {
        if (meetingModel.tabIndex == SubTab.Video.position) {
            if (meetingModel.videoStatesInCurrentPage.isNotEmpty()) {
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
        meetingModel.isLocalVideoStarted = true
        if (meetingModel.isUsingCameraCaptureSource) {
            if (meetingModel.isUsingGpuVideoProcessor) {
                cameraCaptureSource.addVideoSink(gpuVideoProcessor)
                audioVideo.startLocalVideo(gpuVideoProcessor)
            } else if (meetingModel.isUsingCpuVideoProcessor) {
                cameraCaptureSource.addVideoSink(cpuVideoProcessor)
                audioVideo.startLocalVideo(cpuVideoProcessor)
            } else {
                audioVideo.startLocalVideo(cameraCaptureSource)
            }
            cameraCaptureSource.start()
        } else {
            audioVideo.startLocalVideo()
        }
        buttonCamera.setImageResource(R.drawable.button_camera_on)
        selectTab(SubTab.Video.position)
    }

    private fun stopLocalVideo() {
        meetingModel.isLocalVideoStarted = false
        if (meetingModel.isUsingCameraCaptureSource) {
            cameraCaptureSource.stop()
        }
        audioVideo.stopLocalVideo()
        buttonCamera.setImageResource(R.drawable.button_camera)
        selectTab(SubTab.Video.position)
    }

    private fun onVideoPageUpdated() {
        // Recalculate videos in the current page and notify videoTileAdapter
        meetingModel.updateVideoStatesInCurrentPage()
        revalidateVideoPageIndex()
        videoTileAdapter.notifyDataSetChanged()

        // Pause/Resume remote videos accordingly based on videoTileState and the tab that user is on
        meetingModel.remoteVideoTileStates.forEach {
            // Resume paused videos in the current page if user is on Video tab and it was not manually paused by user
            if (meetingModel.tabIndex == SubTab.Video.position && meetingModel.videoStatesInCurrentPage.contains(it) && !meetingModel.userPausedVideoTileIds.contains(it.videoTileState.tileId)) {
                if (it.videoTileState.pauseState == VideoPauseState.PausedByUserRequest) {
                    audioVideo.resumeRemoteVideoTile(it.videoTileState.tileId)
                }
            } else {
                if (it.videoTileState.pauseState != VideoPauseState.PausedByUserRequest) {
                    audioVideo.pauseRemoteVideoTile(it.videoTileState.tileId)
                }
            }
        }

        // update video pagination control buttons states
        prevVideoPageButton.isEnabled = meetingModel.canGoToPrevVideoPage()
        nextVideoPageButton.isEnabled = meetingModel.canGoToNextVideoPage()
    }

    private fun resumeAllContentSharesExceptUserPausedVideos() {
        meetingModel.currentScreenTiles.forEach {
            if (!meetingModel.userPausedVideoTileIds.contains(it.videoTileState.tileId) && it.videoTileState.pauseState == VideoPauseState.PausedByUserRequest) {
                audioVideo.resumeRemoteVideoTile(it.videoTileState.tileId)
            }
        }
    }

    private fun pauseAllRemoteVideos() {
        meetingModel.remoteVideoTileStates.forEach {
            audioVideo.pauseRemoteVideoTile(it.videoTileState.tileId)
        }
    }

    private fun pauseAllContentShares() {
        meetingModel.currentScreenTiles.forEach {
            audioVideo.pauseRemoteVideoTile(it.videoTileState.tileId)
        }
    }

    private fun revalidateVideoPageIndex() {
        while (meetingModel.canGoToPrevVideoPage() && meetingModel.remoteVideoCountInCurrentPage() == 0) {
            meetingModel.currentVideoPageIndex -= 1
            onVideoPageUpdated()
        }
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

    private fun showVideoTile(tileState: VideoTileState) {
        val videoCollectionTile = createVideoCollectionTile(tileState)
        if (tileState.isContent) {
            meetingModel.currentScreenTiles.add(videoCollectionTile)
            screenTileAdapter.notifyDataSetChanged()

            // Currently not in the Screen tab, no need to render the video tile
            if (meetingModel.tabIndex != SubTab.Screen.position) {
                audioVideo.pauseRemoteVideoTile(tileState.tileId)
            }
        } else {
            if (tileState.isLocalTile) {
                meetingModel.localVideoTileState = videoCollectionTile
                onVideoPageUpdated()
            } else {
                meetingModel.remoteVideoTileStates.add(videoCollectionTile)
                onVideoPageUpdated()

                // Currently not in the Video tab, no need to render the video tile
                if (meetingModel.tabIndex != SubTab.Video.position) {
                    audioVideo.pauseRemoteVideoTile(tileState.tileId)
                }
            }
        }
    }

    private fun canShowMoreRemoteScreenTile(): Boolean {
        // only show 1 screen share tile
        return meetingModel.currentScreenTiles.isEmpty()
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        val attendeeName = meetingModel.currentRoster[attendeeId]?.attendeeName ?: ""
        return VideoCollectionTile(attendeeName, tileState, null)
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
        // Start Voice Focus as soon as audio session started
        setVoiceFocusEnabled(true)
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "reconnecting: $reconnecting"
        )

        val cachedDevice = (activity as MeetingActivity).getCachedDevice()
        cachedDevice?.let {
            audioVideo.chooseAudioDevice(it)
            audioDeviceManager.setActiveAudioDevice(it)
            (activity as MeetingActivity).resetCachedDevice()
        }
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
            endMeeting()
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
                "Video tile added, tileId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}" +
                        ", isContent ${tileState.isContent} with size ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight}"
            )
            if (tileState.isContent) {
                if (meetingModel.currentScreenTiles.none { it.videoTileState.tileId == tileState.tileId } && canShowMoreRemoteScreenTile()) {
                    showVideoTile(tileState)
                }
            } else {
                if (tileState.isLocalTile) {
                    showVideoTile(tileState)
                } else if (meetingModel.remoteVideoTileStates.none { it.videoTileState.tileId == tileState.tileId }) {
                    showVideoTile(tileState)
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
                "Video track removed, tileId: $tileId, attendeeId: ${tileState.attendeeId}"
            )
            audioVideo.unbindVideoView(tileId)
            if (tileState.isContent) {
                meetingModel.currentScreenTiles.removeAll { it.videoTileState.tileId == tileId }
                screenTileAdapter.notifyDataSetChanged()
            } else {
                if (meetingModel.localVideoTileState?.videoTileState?.tileId == tileId) {
                    meetingModel.localVideoTileState = null
                } else {
                    meetingModel.remoteVideoTileStates.removeAll { it.videoTileState.tileId == tileId }
                }
                onVideoPageUpdated()
            }
            refreshNoVideosOrScreenShareAvailableText()
        }
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        if (tileState.pauseState == VideoPauseState.PausedForPoorConnection) {
            val attendeeName =
                meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
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
        val attendeeName = meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
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
        videoTileAdapter.notifyDataSetChanged()
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
                    getAttendeeName(dataMessage.senderAttendeeId, dataMessage.senderExternalUserId),
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

    private fun endMeeting() {
        if (meetingModel.localVideoTileState != null) {
            audioVideo.unbindVideoView(meetingModel.localTileId)
        }
        meetingModel.remoteVideoTileStates.forEach {
            audioVideo.unbindVideoView(it.videoTileState.tileId)
        }
        listener.onLeaveMeeting()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceDialog?.dismiss()
        unsubscribeFromAttendeeChangeHandlers()
    }

    // Handle backgrounded app
    override fun onStart() {
        super.onStart()
        if (meetingModel.isLocalVideoStarted) {
            audioVideo.startLocalVideo()
        }
        audioVideo.startRemoteVideo()
    }

    override fun onStop() {
        super.onStop()
        if (meetingModel.isLocalVideoStarted) {
            audioVideo.stopLocalVideo()
        }
        audioVideo.stopRemoteVideo()
    }
}
