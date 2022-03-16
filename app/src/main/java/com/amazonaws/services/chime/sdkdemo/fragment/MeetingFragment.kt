/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsObserver
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.toJsonString
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.PrimaryMeetingPromotionObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.Transcript
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptEvent
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptItem
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptionStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptionStatusType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerpolicy.DefaultActiveSpeakerPolicy
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareStatus
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.ObservableMetric
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPriority
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CaptureSourceError
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CaptureSourceObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultScreenCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.internal.AttendeeStatus
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DefaultBackOffRetry
import com.amazonaws.services.chime.sdk.meetings.internal.utils.HttpUtils
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.TranscriptEventObserver
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessage
import com.amazonaws.services.chime.sdk.meetings.realtime.datamessage.DataMessageObserver
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatusCode
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.activity.TranscriptionConfigActivity
import com.amazonaws.services.chime.sdkdemo.adapter.CaptionAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.DeviceAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MessageAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.MetricAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.RosterAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.VideoAdapter
import com.amazonaws.services.chime.sdkdemo.adapter.VideoDiffCallback
import com.amazonaws.services.chime.sdkdemo.data.Caption
import com.amazonaws.services.chime.sdkdemo.data.JoinMeetingResponse
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.device.AudioDeviceManager
import com.amazonaws.services.chime.sdkdemo.device.ScreenShareManager
import com.amazonaws.services.chime.sdkdemo.model.MeetingModel
import com.amazonaws.services.chime.sdkdemo.service.ScreenCaptureService
import com.amazonaws.services.chime.sdkdemo.utils.CpuVideoProcessor
import com.amazonaws.services.chime.sdkdemo.utils.GpuVideoProcessor
import com.amazonaws.services.chime.sdkdemo.utils.PostLogger
import com.amazonaws.services.chime.sdkdemo.utils.encodeURLParam
import com.amazonaws.services.chime.sdkdemo.utils.formatTimestamp
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import java.net.URL
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MeetingFragment : Fragment(),
    RealtimeObserver, AudioVideoObserver, VideoTileObserver,
    MetricsObserver, ActiveSpeakerObserver, DeviceChangeObserver, DataMessageObserver,
    ContentShareObserver, EventAnalyticsObserver, TranscriptEventObserver, PrimaryMeetingPromotionObserver {
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val mutex = Mutex()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val meetingModel: MeetingModel by lazy { ViewModelProvider(this)[MeetingModel::class.java] }
    private var deviceDialog: AlertDialog? = null
    private var screenShareManager: ScreenShareManager? = null
    private val gson = Gson()
    private val appName = "SDKEvents"

    private val SCROLL_TO_END_DELAY: Long = 100

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var powerManager: PowerManager
    private lateinit var credentials: MeetingSessionCredentials
    // Cached for reuse and making sure we don't immediately stop content share
    private var primaryMeetingCredentials: MeetingSessionCredentials? = null
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private lateinit var gpuVideoProcessor: GpuVideoProcessor
    private lateinit var cpuVideoProcessor: CpuVideoProcessor
    private lateinit var eglCoreFactory: EglCoreFactory
    private lateinit var listener: RosterViewEventListener
    private lateinit var postLogger: PostLogger

    override val scoreCallbackIntervalMs: Int? get() = 1000

    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val SCREEN_CAPTURE_REQUEST_CODE = 2
    private val TAG = "MeetingFragment"

    // Append to attendee name if it's for content share
    private val CONTENT_NAME_SUFFIX = "<<Content>>"

    private val DATA_MESSAGE_TOPIC = "chat"
    private val DATA_MESSAGE_LIFETIME_MS = 300000

    private var screenshareServiceConnection: ServiceConnection? = null
    private var isBound: Boolean = false

    private var primaryExternalMeetingId: String? = null
    private var hasJoinedPrimaryMeeting = false

    enum class SubTab(val position: Int) {
        Attendees(0),
        Chat(1),
        Video(2),
        Screen(3),
        Captions(4),
        Metrics(5)
    }

    private lateinit var noVideoOrScreenShareAvailable: TextView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSendChat: ImageButton
    private lateinit var buttonMute: ImageButton
    private lateinit var buttonSpeaker: ImageButton
    private lateinit var buttonCamera: ImageButton
    private lateinit var deviceAlertDialogBuilder: AlertDialog.Builder
    private lateinit var additionalOptionsAlertDialogBuilder: AlertDialog.Builder
    private lateinit var viewChat: LinearLayout
    private lateinit var recyclerViewMetrics: RecyclerView
    private lateinit var recyclerViewRoster: RecyclerView
    private lateinit var recyclerViewCaptions: RecyclerView
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
    private lateinit var captionAdapter: CaptionAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var audioDeviceManager: AudioDeviceManager

    companion object {
        fun newInstance(
            meetingId: String,
            audioVideoConfig: AudioVideoConfiguration,
            meetingEndpointUrl: String
        ): MeetingFragment {
            val fragment = MeetingFragment()

            fragment.arguments = bundleOf(
                HomeActivity.MEETING_ID_KEY to meetingId,
                HomeActivity.AUDIO_MODE_KEY to audioVideoConfig.audioMode.value,
                HomeActivity.MEETING_ENDPOINT_KEY to meetingEndpointUrl
            )
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
        primaryExternalMeetingId = activity.getPrimaryExternalMeetingId()
        audioVideo = activity.getAudioVideo()
        eglCoreFactory = activity.getEglCoreFactory()
        cameraCaptureSource = activity.getCameraCaptureSource()
        gpuVideoProcessor = activity.getGpuVideoProcessor()
        cpuVideoProcessor = activity.getCpuVideoProcessor()
        screenShareManager = activity.getScreenShareManager()
        audioDeviceManager = AudioDeviceManager(audioVideo)

        val meetingEndpointUrl = arguments?.getString(HomeActivity.MEETING_ENDPOINT_KEY) as String
        postLogger = PostLogger(
            appName,
            activity.getMeetingSessionConfiguration(),
            "${meetingEndpointUrl}log_meeting_event",
            LogLevel.INFO
        )

        mediaProjectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager

        view.findViewById<TextView>(R.id.textViewMeetingId)?.text = arguments?.getString(
            HomeActivity.MEETING_ID_KEY
        ) as String
        setupButtonsBar(view)
        setupSubViews(view)
        setupTab(view)
        setupAudioDeviceSelectionDialog()
        setupAdditionalOptionsDialog()

        if (!primaryExternalMeetingId.isNullOrEmpty()) {
            updateUiForPromotionStatus(false)
        }

        noVideoOrScreenShareAvailable = view.findViewById(R.id.noVideoOrScreenShareAvailable)
        refreshNoVideosOrScreenShareAvailableText()

        selectTab(meetingModel.tabIndex)
        setupAudioVideoFacadeObservers()
        val audioMode = arguments?.getInt(HomeActivity.AUDIO_MODE_KEY)?.let { intValue ->
            AudioMode.from(intValue, defaultAudioMode = AudioMode.Stereo48K)
        } ?: AudioMode.Stereo48K
        val audioVideoConfig = AudioVideoConfiguration(audioMode = audioMode)
        // Start Audio Video
        audioVideo.start(audioVideoConfig)
        audioVideo.startRemoteVideo()
        return view
    }

    private fun setupButtonsBar(view: View) {
        buttonMute = view.findViewById(R.id.buttonMute)
        buttonMute.setImageResource(if (meetingModel.isMuted) R.drawable.button_mute_on else R.drawable.button_mute)
        buttonMute.setOnClickListener { toggleMute() }

        buttonSpeaker = view.findViewById(R.id.buttonSpeaker)
        buttonSpeaker.setOnClickListener { toggleSpeaker() }

        buttonCamera = view.findViewById(R.id.buttonCamera)
        buttonCamera.setImageResource(if (meetingModel.isCameraOn) R.drawable.button_camera_on else R.drawable.button_camera)
        buttonCamera.setOnClickListener { toggleVideo() }

        view.findViewById<ImageButton>(R.id.buttonMore)
            ?.setOnClickListener { toggleAdditionalOptionsMenu() }

        view.findViewById<ImageButton>(R.id.buttonLeave)
            ?.setOnClickListener { endMeeting() }
    }

    private fun updateUiForPromotionStatus(promoted: Boolean) {
        val buttons = listOf(
            buttonMute,
            buttonCamera,
            buttonSendChat
        )

        buttons.forEach {
            if (!promoted) {
                it.alpha = 0.2f
                it.isClickable = false
            } else {
                it.alpha = 1f
                it.isClickable = true
            }
        }
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
            meetingModel.remoteVideoSourceConfigurations,
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
                meetingModel.remoteVideoSourceConfigurations,
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
        buttonSendChat = view.findViewById(R.id.buttonSubmitMessage)
        buttonSendChat.let {
            it.setOnClickListener { sendMessage() }
        }

        viewChat.visibility = View.GONE

        // Caption
        recyclerViewCaptions = view.findViewById(R.id.recyclerViewCaptions)
        recyclerViewCaptions.layoutManager = LinearLayoutManager(activity)
        captionAdapter = CaptionAdapter(meetingModel.currentCaptions)
        recyclerViewCaptions.adapter = captionAdapter
        recyclerViewCaptions.visibility = View.GONE

        // orientation change scrolls to latest caption
        Handler(Looper.getMainLooper()).postDelayed({
            scrollToLastCaption()
        }, SCROLL_TO_END_DELAY)
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
        meetingModel.localVideoTileState?.setRenderViewVisibility(visibility)
        meetingModel.remoteVideoTileStates.forEach { it.setRenderViewVisibility(visibility) }
    }

    private fun setScreenSurfaceViewsVisibility(visibility: Int) {
        meetingModel.currentScreenTiles.forEach { it.setRenderViewVisibility(visibility) }
    }

    private fun showViewAt(index: Int) {
        recyclerViewRoster.visibility = View.GONE
        viewChat.visibility = View.GONE
        viewVideo.visibility = View.GONE
        recyclerViewScreenShareCollection.visibility = View.GONE
        recyclerViewMetrics.visibility = View.GONE
        recyclerViewCaptions.visibility = View.GONE

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
            SubTab.Captions.position -> {
                recyclerViewCaptions.visibility = View.VISIBLE
                scrollToLastCaption()
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
        return if (isLandscapeMode(activity)) {
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
            audioDeviceManager
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
        additionalOptionsAlertDialogBuilder.setOnDismissListener {
            meetingModel.isAdditionalOptionsDialogOn = false
        }

        if (meetingModel.isAdditionalOptionsDialogOn) {
            additionalOptionsAlertDialogBuilder.create().show()
        }
    }

    private fun refreshAdditionalOptionsDialogItems() {
        if (!primaryExternalMeetingId.isNullOrEmpty() && !hasJoinedPrimaryMeeting) {
            val additionalToggles = arrayOf(
                context?.getString(R.string.promote_to_primary_meeting)
            )

            additionalOptionsAlertDialogBuilder.setItems(additionalToggles) { _, which ->
                when (which) {
                    0 -> promoteToPrimaryMeeting()
                }
            }
            return
        }
        val isVoiceFocusEnabled = audioVideo.realtimeIsVoiceFocusEnabled()
        val additionalToggles = mutableListOf(
            context?.getString(if (meetingModel.isSharingContent) R.string.disable_screen_capture_source else R.string.enable_screen_capture_source),
            context?.getString(if (isVoiceFocusEnabled) R.string.disable_voice_focus else R.string.enable_voice_focus),
            context?.getString(if (meetingModel.isLiveTranscriptionEnabled) R.string.disable_live_transcription else R.string.enable_live_transcription),
            context?.getString(if (cameraCaptureSource.torchEnabled) R.string.disable_flashlight else R.string.enable_flashlight),
            context?.getString(if (meetingModel.isUsingCpuVideoProcessor) R.string.disable_cpu_filter else R.string.enable_cpu_filter),
            context?.getString(if (meetingModel.isUsingGpuVideoProcessor) R.string.disable_gpu_filter else R.string.enable_gpu_filter),
            context?.getString(if (meetingModel.isUsingCameraCaptureSource) R.string.disable_custom_capture_source else R.string.enable_custom_capture_source)
        )
        if (!primaryExternalMeetingId.isNullOrEmpty()) {
            additionalToggles.add(context?.getString(R.string.demote_from_primary_meeting))
        }

        additionalOptionsAlertDialogBuilder.setItems(additionalToggles.toTypedArray()) { _, which ->
            when (which) {
                0 -> toggleScreenCapture()
                1 -> setVoiceFocusEnabled(!isVoiceFocusEnabled)
                2 -> toggleLiveTranscription(
                    arguments?.getString(HomeActivity.MEETING_ID_KEY) as String,
                    arguments?.getString(HomeActivity.MEETING_ENDPOINT_KEY) as String
                )
                3 -> toggleFlashlight()
                4 -> toggleCpuDemoFilter()
                5 -> toggleGpuDemoFilter()
                6 -> toggleCustomCaptureSource()
                7 -> demoteFromPrimaryMeeting() // May not be accessible
            }
        }
    }

    private fun promoteToPrimaryMeeting() {
        if (primaryMeetingCredentials == null) {
            primaryMeetingCredentials = runBlocking { getJoinResponseForPrimaryMeeting() }
        }
        primaryMeetingCredentials?.let {
            audioVideo.promoteToPrimaryMeeting(it, this)
        } ?: run {
            logger.error(TAG, "Could not retrieve primary meeting credentials")
        }
    }

    private suspend fun getJoinResponseForPrimaryMeeting(): MeetingSessionCredentials? {
        val meetingEndpointUrl = arguments?.getString(HomeActivity.MEETING_ENDPOINT_KEY) as String
        var url = if (meetingEndpointUrl.endsWith("/")) meetingEndpointUrl else "$meetingEndpointUrl/"
        val attendeeName = getAttendeeName(credentials.attendeeId, credentials.externalUserId)
        url = "${url}join?title=${encodeURLParam(primaryExternalMeetingId)}&name=promoted-${encodeURLParam(attendeeName)}"
        url += "&region=region"
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)
        val responseData = if (response.httpException == null) {
            response.data
        } else {
            logger.error(TAG, "Unable to request primary meeting attendee credentials. ${response.httpException}")
            null
        }
        return try {
            val joinMeetingResponse = gson.fromJson(responseData, JoinMeetingResponse::class.java)
            MeetingSessionConfiguration(
                CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
                CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
                ::dummyUrlRewriter
            ).credentials
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Error creating session configuration: ${exception.localizedMessage}"
            )
            null
        }
    }

    private fun dummyUrlRewriter(url: String): String {
        return url
    }

    private fun demoteFromPrimaryMeeting() {
        audioVideo.demoteFromPrimaryMeeting()
    }

    override fun onPrimaryMeetingPromotion(status: MeetingSessionStatus) {
        logger.info(TAG, "Completed primary meeting promotion with status $status")
        if (status.statusCode == MeetingSessionStatusCode.OK) {
            notifyHandler("Promoted to primary meeting")
            hasJoinedPrimaryMeeting = true
            updateUiForPromotionStatus(true)
        } else {
            notifyHandler("Promotion to primary meeting failed")
        }
    }

    override fun onPrimaryMeetingDemotion(status: MeetingSessionStatus) {
        notifyHandler(
            "Demoted from primary meeting for reason: ${status.statusCode}"
        )
        hasJoinedPrimaryMeeting = false
        updateUiForPromotionStatus(false)
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
        onAttendeesJoinedWithStatus(attendeeInfo, AttendeeStatus.Joined)
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

    private fun onAttendeesJoinedWithStatus(
        attendeeInfo: Array<AttendeeInfo>,
        status: AttendeeStatus
    ) {
        uiScope.launch {
            mutex.withLock {
                attendeeInfo.forEach { (attendeeId, externalUserId) ->
                    if (DefaultModality(attendeeId).hasModality(ModalityType.Content) &&
                        !isSelfAttendee(attendeeId) &&
                        meetingModel.isSharingContent
                    ) {
                        audioVideo.stopContentShare()
                        screenShareManager?.stop()
                        val name =
                            meetingModel.currentRoster[DefaultModality(attendeeId).base()]?.attendeeName
                                ?: ""
                        notifyHandler("$name took over the screen share")
                    }
                    meetingModel.currentRoster.getOrPut(
                        attendeeId,
                        {
                            RosterAttendee(
                                attendeeId,
                                getAttendeeName(attendeeId, externalUserId),
                                attendeeStatus = status
                            )
                        }
                    )
                }

                rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun getAttendeeName(attendeeId: String, externalUserId: String): String {
        if ((attendeeId.isEmpty() || externalUserId.isEmpty()) || !externalUserId.contains('#')) {
            return "<UNKNOWN>"
        }
        val attendeeName = externalUserId.split('#')[1]

        return if (DefaultModality(attendeeId).hasModality(ModalityType.Content)) {
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
        meetingModel.currentMediaDevices =
            audioVideo.listAudioDevices().filter { it.type != MediaDeviceType.OTHER }
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
        } else {
            notifyHandler("Failed to $action Voice Focus")
        }
    }

    private fun toggleLiveTranscription(meetingId: String, meetingEndpointUrl: String) {
        if (meetingModel.isLiveTranscriptionEnabled) {
            uiScope.launch {
                val transcriptionResponseJson: String? =
                    disableMeetingTranscription(meetingId, meetingEndpointUrl)

                if (transcriptionResponseJson == null) {
                    notifyHandler(getString(R.string.user_notification_transcription_stop_error))
                } else {
                    notifyHandler(getString(R.string.user_notification_transcription_stop_success))
                }
            }
        } else {
            val intent = Intent(context, TranscriptionConfigActivity::class.java)
            intent.putExtra(HomeActivity.MEETING_ID_KEY, meetingId)
            intent.putExtra(HomeActivity.MEETING_ENDPOINT_KEY, meetingEndpointUrl)
            startActivity(intent)
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
        refreshAdditionalOptionsDialogItems()
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

    private fun toggleScreenCapture() {
        if (meetingModel.isSharingContent) {
            audioVideo.stopContentShare()
            screenShareManager?.stop(isBound)
        } else {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                SCREEN_CAPTURE_REQUEST_CODE
            )
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
    }

    private fun stopLocalVideo() {
        meetingModel.isLocalVideoStarted = false
        if (meetingModel.isUsingCameraCaptureSource) {
            cameraCaptureSource.stop()
        }
        audioVideo.stopLocalVideo()
        buttonCamera.setImageResource(R.drawable.button_camera)
    }

    private fun addTranscriptEvent(transcriptEvent: TranscriptEvent) {
        when (transcriptEvent) {
            is TranscriptionStatus -> {
                val eventTime = formatTimestamp(transcriptEvent.eventTimeMs)
                val content =
                    "Live transcription ${transcriptEvent.type} at $eventTime in ${transcriptEvent.transcriptionRegion} with configuration: ${transcriptEvent.transcriptionConfiguration}"
                val caption = Caption(null, false, content)
                if (transcriptEvent.type == TranscriptionStatusType.Started) {
                    meetingModel.isLiveTranscriptionEnabled = true
                }
                if (transcriptEvent.type == TranscriptionStatusType.Stopped) {
                    meetingModel.isLiveTranscriptionEnabled = false
                }
                meetingModel.currentCaptions.add(caption)
            }
            is Transcript -> {
                val entitySet = mutableSetOf<String>()
                transcriptEvent.results.forEach { result ->
                    val alternative = result.alternatives.firstOrNull() ?: return
                    // for simplicity and demo purposes, assume each result only contains transcripts from
                    // the same speaker, which matches our observation with current transcription service behavior.
                    // More complicated UI logic can be achieved by iterating through each item
                    val speakerName: String
                    val item: TranscriptItem? = alternative.items.firstOrNull()
                    speakerName = if (item == null) {
                        logger.debug(
                            TAG,
                            "Empty speaker name due to empty items array for result: ${result.resultId}"
                        )
                        "<UNKNOWN>"
                    } else {
                        getAttendeeName(item.attendee.attendeeId, item.attendee.externalUserId)
                    }
                    val caption: Caption
                    val entities = alternative.entities
                    caption = if (entities == null || result.isPartial) {
                        Caption(speakerName, result.isPartial, alternative.transcript, alternative.items)
                    } else {
                        entities.forEach { entity ->
                            entitySet.addAll(entity.content.split(" "))
                        }
                        Caption(speakerName, result.isPartial, alternative.transcript, alternative.items, entitySet)
                    }
                    val captionIndex = meetingModel.currentCaptionIndices[result.resultId]
                    if (captionIndex != null) {
                        // update existing (partial) caption if exists
                        meetingModel.currentCaptions[captionIndex] = caption
                    } else {
                        meetingModel.currentCaptions.add(caption)
                        meetingModel.currentCaptionIndices[result.resultId] =
                            meetingModel.currentCaptions.count() - 1
                    }
                }
            }
        }
    }

    private suspend fun disableMeetingTranscription(
        meetingId: String?,
        meetingEndpointUrl: String
    ): String? {
        val meetingUrl =
            if (meetingEndpointUrl.endsWith("/")) meetingEndpointUrl else meetingEndpointUrl.plus("/")
        val url = "${meetingUrl}stop_transcription?title=${encodeURLParam(meetingId)}"
        val response = HttpUtils.post(URL(url), "", DefaultBackOffRetry(), logger)
        return if (response.httpException == null) {
            response.data
        } else {
            logger.error(TAG, "Error sending stop transcription request ${response.httpException}")
            null
        }
    }

    private fun onVideoPageUpdated() {
        val oldList = mutableListOf<VideoCollectionTile>()
        oldList.addAll(meetingModel.videoStatesInCurrentPage)
        val removedList: ArrayList<RemoteVideoSource> = arrayListOf()
        for (videoState in oldList) {
            for ((key) in meetingModel.remoteVideoSourceConfigurations) {
                if (videoState.videoTileState.attendeeId == key.attendeeId) {
                    removedList.add(key)
                }
            }
        }
        val removedSources: Array<RemoteVideoSource> = removedList.toTypedArray()
        // Recalculate videos in the current page and notify videoTileAdapter
        meetingModel.updateVideoStatesInCurrentPage()

        revalidateVideoPageIndex()

        val newList = mutableListOf<VideoCollectionTile>()
        newList.addAll(meetingModel.videoStatesInCurrentPage)
        val updatedSources: MutableMap<RemoteVideoSource, VideoSubscriptionConfiguration> =
            mutableMapOf()
        for (videoState in newList) {
            for ((key, value) in meetingModel.remoteVideoSourceConfigurations) {
                if (videoState.videoTileState.attendeeId == key.attendeeId) {
                    updatedSources[key] = value
                }
            }
        }

        val videoDiffCallback = VideoDiffCallback(oldList, newList)
        val videoDiffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(videoDiffCallback)

        videoDiffResult.dispatchUpdatesTo(videoTileAdapter)

        // subscribe to updated sources with highest priority and unsubscribed sources in prev page
        audioVideo.updateVideoSourceSubscriptions(updatedSources, removedSources)

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
                        context,
                        getString(R.string.user_notification_permission_error),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startLocalVideo()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SCREEN_CAPTURE_REQUEST_CODE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    context,
                    getString(R.string.user_notification_screen_share_permission_error),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                data?.let { startScreenShare(resultCode, it, requireContext()) }
            }
        }
    }

    private fun startScreenShare(resultCode: Int, data: Intent, fragmentContext: Context) {
        screenshareServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val screenCaptureSource = DefaultScreenCaptureSource(
                    fragmentContext,
                    logger,
                    DefaultSurfaceTextureCaptureSourceFactory(
                        logger,
                        eglCoreFactory
                    ),
                    resultCode,
                    data
                )
                isBound = true

                val screenCaptureSourceObserver = object : CaptureSourceObserver {
                    override fun onCaptureStarted() {
                        screenShareManager?.let { source ->
                            audioVideo.startContentShare(source)
                        }
                    }

                    override fun onCaptureStopped() {
                        notifyHandler("Screen capture stopped")
                    }

                    override fun onCaptureFailed(error: CaptureSourceError) {
                        notifyHandler("Screen capture failed with error $error")
                        audioVideo.stopContentShare()
                    }
                }

                screenShareManager = ScreenShareManager(screenCaptureSource, fragmentContext)
                screenShareManager?.screenCaptureConnectionService = screenshareServiceConnection
                screenShareManager?.addObserver(screenCaptureSourceObserver)
                screenShareManager?.start()
                (activity as MeetingActivity).setScreenShareManager(screenShareManager)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                isBound = false
            }
        }

        fragmentContext.startService(
            Intent(
                fragmentContext,
                ScreenCaptureService::class.java
            ).also { intent ->
                screenshareServiceConnection?.let {
                    context?.bindService(
                        intent,
                        it,
                        Context.BIND_AUTO_CREATE
                    )
                }
            })
    }

    override fun onContentShareStarted() {
        notifyHandler("Content share started")
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name
        )
        meetingModel.isSharingContent = true
    }

    override fun onContentShareStopped(status: ContentShareStatus) {
        notifyHandler("Content share stopped with status ${status.statusCode}")
        logWithFunctionName(
            object {}.javaClass.enclosingMethod?.name,
            "$status"
        )
        meetingModel.isSharingContent = false
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

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        val attendeeName = meetingModel.currentRoster[attendeeId]?.attendeeName ?: ""
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
        // Start Amazon Voice Focus as soon as audio session started
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
        listener.onLeaveMeeting()
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
        when (sessionStatus.statusCode) {
            MeetingSessionStatusCode.VideoAtCapacityViewOnly -> {
                notifyHandler("Currently cannot enable video in meeting")
                stopLocalVideo()
                meetingModel.isCameraOn = !meetingModel.isCameraOn
                refreshNoVideosOrScreenShareAvailableText()
            }
        }
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

    override fun onRemoteVideoSourceAvailable(sources: List<RemoteVideoSource>) {
        for (source in sources) {
            meetingModel.remoteVideoSourceConfigurations[source] =
                VideoSubscriptionConfiguration(VideoPriority.Medium, VideoResolution.Medium)
        }
        // Use the default auto subscribe behavior
    }

    override fun onRemoteVideoSourceUnavailable(sources: List<RemoteVideoSource>) {
        sources.forEach { meetingModel.remoteVideoSourceConfigurations.remove(it) }
    }

    override fun onVideoTileAdded(tileState: VideoTileState) {
        uiScope.launch {
            logger.info(
                TAG,
                "Video tile added, tileId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}" +
                        ", isContent ${tileState.isContent} with size ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight}"
            )
            if (tileState.isContent && meetingModel.currentScreenTiles.none { it.videoTileState.tileId == tileState.tileId }) {
                showVideoTile(tileState)
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
            val collection =
                if (tileState.isContent) meetingModel.currentScreenTiles else meetingModel.remoteVideoTileStates
            collection.find { it.videoTileState.tileId == tileState.tileId }.apply {
                this?.setPauseMessageVisibility(View.VISIBLE)
            }
            val attendeeName =
                meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
            logWithFunctionName(
                object {}.javaClass.enclosingMethod?.name,
                "$attendeeName video paused"
            )
        }
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        val collection =
            if (tileState.isContent) meetingModel.currentScreenTiles else meetingModel.remoteVideoTileStates
        collection.find { it.videoTileState.tileId == tileState.tileId }.apply {
            this?.setPauseMessageVisibility(View.INVISIBLE)
        }
        val attendeeName = meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
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
                meetingModel.currentMetrics.clear()
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
                    isSelfAttendee(dataMessage.senderAttendeeId)
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

    private fun scrollToLastCaption() {
        if (meetingModel.currentCaptions.isNotEmpty()) {
            recyclerViewCaptions.scrollToPosition(meetingModel.currentCaptions.size - 1)
        }
    }

    private fun isSelfAttendee(attendeeId: String): Boolean {
        return DefaultModality(attendeeId).base() == credentials.attendeeId || DefaultModality(attendeeId).base() == primaryMeetingCredentials?.attendeeId
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

    private fun setupAudioVideoFacadeObservers() {
        audioVideo.addAudioVideoObserver(this)
        audioVideo.addDeviceChangeObserver(this)
        audioVideo.addMetricsObserver(this)
        audioVideo.addRealtimeObserver(this)
        audioVideo.addRealtimeDataMessageObserver(DATA_MESSAGE_TOPIC, this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.addActiveSpeakerObserver(DefaultActiveSpeakerPolicy(), this)
        audioVideo.addContentShareObserver(this)
        audioVideo.addEventAnalyticsObserver(this)
        audioVideo.addRealtimeTranscriptEventObserver(this)
    }

    private fun removeAudioVideoFacadeObservers() {
        audioVideo.removeAudioVideoObserver(this)
        audioVideo.removeDeviceChangeObserver(this)
        audioVideo.removeMetricsObserver(this)
        audioVideo.removeRealtimeObserver(this)
        audioVideo.removeRealtimeDataMessageObserverFromTopic(DATA_MESSAGE_TOPIC)
        audioVideo.removeVideoTileObserver(this)
        audioVideo.removeActiveSpeakerObserver(this)
        audioVideo.removeContentShareObserver(this)
        audioVideo.removeRealtimeTranscriptEventObserver(this)
    }

    private fun endMeeting() {
        if (meetingModel.localVideoTileState != null) {
            audioVideo.unbindVideoView(meetingModel.localTileId)
        }
        meetingModel.remoteVideoTileStates.forEach {
            audioVideo.unbindVideoView(it.videoTileState.tileId)
        }
        meetingModel.currentScreenTiles.forEach {
            audioVideo.unbindVideoView(it.videoTileState.tileId)
        }
        audioVideo.stopLocalVideo()
        audioVideo.stopContentShare()
        audioVideo.stopRemoteVideo()
        audioVideo.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceDialog?.dismiss()
        removeAudioVideoFacadeObservers()
    }

    // Handle backgrounded app
    override fun onStart() {
        super.onStart()
        if (meetingModel.wasLocalVideoStarted) {
            startLocalVideo()
        }
        audioVideo.startRemoteVideo()
    }

    override fun onStop() {
        super.onStop()
        meetingModel.wasLocalVideoStarted = meetingModel.isLocalVideoStarted
        if (meetingModel.wasLocalVideoStarted) {
            stopLocalVideo()
        }
        audioVideo.stopRemoteVideo()

        // Turn off screen share when screen locked
        if (meetingModel.isSharingContent && !powerManager.isInteractive) {
            audioVideo.stopContentShare()
            screenShareManager?.stop(isBound)
        }
    }

    override fun onEventReceived(name: EventName, attributes: EventAttributes) {
        // Store the logs
        attributes.putAll(audioVideo.getCommonEventAttributes())
        postLogger.info(
            TAG, gson.toJson(
                mutableMapOf(
                    "name" to name,
                    "attributes" to attributes
                )
            )
        )

        logger.info(TAG, "$name ${attributes.toJsonString()}")
        when (name) {
            EventName.meetingStartSucceeded ->
                logger.info(
                    TAG,
                    "Meeting started on : ${audioVideo.getCommonEventAttributes().toJsonString()}"
                )
            EventName.meetingEnded, EventName.meetingFailed -> {
                logger.info(TAG, "Meeting history: ${gson.toJson(audioVideo.getMeetingHistory())}")
                postLogger.publishLog(TAG)
            }
            else -> Unit
        }
    }

    override fun onTranscriptEventReceived(transcriptEvent: TranscriptEvent) {
        uiScope.launch {
            addTranscriptEvent(transcriptEvent = transcriptEvent)
            scrollToLastCaption()

            captionAdapter.notifyDataSetChanged()
        }
    }
}
