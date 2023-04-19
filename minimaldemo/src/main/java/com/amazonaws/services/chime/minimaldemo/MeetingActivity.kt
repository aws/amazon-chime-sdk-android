/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.services.chime.minimaldemo.adapter.RosterAdapter
import com.amazonaws.services.chime.minimaldemo.adapter.VideoAdapter
import com.amazonaws.services.chime.minimaldemo.data.JoinMeetingResponse
import com.amazonaws.services.chime.minimaldemo.data.RosterAttendee
import com.amazonaws.services.chime.minimaldemo.data.VideoCollectionTile
import com.amazonaws.services.chime.minimaldemo.databinding.ActivityMeetingBinding
import com.amazonaws.services.chime.minimaldemo.models.MeetingSessionViewModel
import com.amazonaws.services.chime.minimaldemo.models.MeetingViewModel
import com.amazonaws.services.chime.minimaldemo.models.ViewModelFactory
import com.amazonaws.services.chime.minimaldemo.utils.isContentShare
import com.amazonaws.services.chime.minimaldemo.utils.showToast
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeUpdate
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.internal.AttendeeStatus
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MeetingActivity"

class MeetingActivity : AppCompatActivity(), AudioVideoObserver, RealtimeObserver, VideoTileObserver {

    private lateinit var binding: ActivityMeetingBinding
    private lateinit var meetingId: String
    private lateinit var audioVideoConfig: AudioVideoConfiguration
    private lateinit var meetingModel: MeetingViewModel
    private lateinit var rosterAdapter: RosterAdapter
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var meetingCredentials: MeetingSessionCredentials

    private val meetingSessionModel: MeetingSessionViewModel by lazy { ViewModelProvider(this)[MeetingSessionViewModel::class.java] }
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val logger = ConsoleLogger()
    private val gson = Gson()

    companion object {
        private val CONTENT_NAME_SUFFIX = "<<Content>>"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val audioMode = intent.extras?.getInt(MainActivity.AUDIO_MODE_KEY)?.let { intValue ->
            AudioMode.from(intValue, defaultAudioMode = AudioMode.Stereo48K)
        } ?: AudioMode.Stereo48K
        audioVideoConfig = AudioVideoConfiguration(audioMode = audioMode)
        meetingId = intent.extras?.getString(MainActivity.MEETING_ID_KEY) as String

        if (savedInstanceState == null) {
            val meetingResponseJson =
                intent.extras?.getString(MainActivity.MEETING_RESPONSE_KEY) as String
            val sessionConfig =
                createSessionConfiguration(meetingResponseJson)
            val meetingSession = sessionConfig?.let {
                logger.info(TAG, "Creating meeting session for meeting Id: $meetingId")
                meetingCredentials = sessionConfig.credentials

                DefaultMeetingSession(
                    it,
                    logger,
                    applicationContext,
                    // Note if the following isn't provided app will (as expected) crash if we use custom video source
                    // since an EglCoreFactory will be internal created and will be using a different shared EGLContext.
                    // However the internal default capture would work fine, since it is initialized using
                    // that internally created default EglCoreFactory, and can be smoke tested by removing this
                    // argument and toggling use of custom video source before starting video
                    meetingSessionModel.eglCoreFactory
                )
            }

            if (meetingSession == null) {
                showToast(getString(R.string.user_notification_meeting_start_error))
                finish()
                return
            }

            meetingModel = ViewModelProvider(
                this,
                ViewModelFactory(meetingSession)
            )[MeetingViewModel::class.java]
            meetingSession.audioVideo.addAudioVideoObserver(this)
            meetingSession.audioVideo.addRealtimeObserver(this)
            meetingSession.audioVideo.addVideoTileObserver(this)

            setupButtons()
            setupRosterView()
            setupVideoView()

            meetingSession.audioVideo.start()
            meetingSession.audioVideo.startRemoteVideo()
        }
    }

    private fun setupButtons() {
        binding.contentMeeting.buttonCamera.setOnClickListener {
            meetingModel.toggleLocalVideo()
            if (meetingModel.isLocalVideoOn) binding.contentMeeting.buttonCamera.setImageResource(R.drawable.button_camera_on)
            else binding.contentMeeting.buttonCamera.setImageResource(R.drawable.button_camera)
        }

        binding.contentMeeting.buttonMute.setOnClickListener {
            meetingModel.toggleMute()
            if (meetingModel.isMuted) binding.contentMeeting.buttonMute.setImageResource(R.drawable.button_mute_on)
            else binding.contentMeeting.buttonMute.setImageResource(R.drawable.button_mute)
        }

        binding.contentMeeting.buttonLeave.setOnClickListener {
            meetingModel.endMeeting()
        }
    }

    private fun setupRosterView() {
        val rosterView = binding.contentMeeting.rosterView
        rosterView.layoutManager = LinearLayoutManager(this)
        rosterAdapter = RosterAdapter(meetingModel.currentRoster.values)
        rosterView.adapter = rosterAdapter
    }

    private fun setupVideoView() {
        val videoView = binding.contentMeeting.videoView
        videoView.layoutManager = GridLayoutManager(this, 2)
        videoAdapter = VideoAdapter(meetingModel.videos, meetingModel, logger)
        videoView.adapter = videoAdapter
    }

    private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null
        return try {
            val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
            MeetingSessionConfiguration(
                CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse),
                CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse)
            )
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Error creating session configuration: ${exception.localizedMessage}"
            )
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
    }

    override fun onAudioSessionCancelledReconnect() {
        logger.info(TAG, "onAudioSessionCancelledReconnect")
    }

    override fun onAudioSessionDropped() {
        logger.info(TAG, "onAudioSessionDropped")
    }

    override fun onAudioSessionStarted(reconnecting: Boolean) {
        logger.info(TAG, "onAudioSessionStarted")
        meetingModel.enableVoiceFocus()
        meetingModel.selectAudioDevice()
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        logger.info(TAG, "onAudioSessionStartedConnecting")
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        logger.info(TAG, "onAudioSessionStopped")
        meetingModel.meetingSession.audioVideo.removeAudioVideoObserver(this)
        meetingModel.meetingSession.audioVideo.removeRealtimeObserver(this)
        meetingModel.meetingSession.audioVideo.removeVideoTileObserver(this)
        onBackPressed()
    }

    override fun onCameraSendAvailabilityUpdated(available: Boolean) {
        logger.info(TAG, "onCameraSendAvailabilityUpdated")
    }

    override fun onConnectionBecamePoor() {
        logger.info(TAG, "onConnectionBecamePoor")
    }

    override fun onConnectionRecovered() {
        logger.info(TAG, "onConnectionRecovered")
    }

    override fun onRemoteVideoSourceAvailable(sources: List<RemoteVideoSource>) {
        logger.info(TAG, "onRemoteVideoSourceAvailable")
    }

    override fun onRemoteVideoSourceUnavailable(sources: List<RemoteVideoSource>) {
        logger.info(TAG, "onRemoteVideoSourceUnavailable")
    }

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        logger.info(TAG, "onVideoSessionStarted")
    }

    override fun onVideoSessionStartedConnecting() {
        logger.info(TAG, "onVideoSessionStartedConnecting")
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        logger.info(TAG, "onVideoSessionStopped")
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (_, externalUserId) ->
            logger.info(TAG, "$externalUserId dropped")
        }
        uiScope.launch {
            attendeeInfo.forEach { (attendeeId, _) ->
                meetingModel.currentRoster.remove(
                    attendeeId
                )
            }

            rosterAdapter.notifyDataSetChanged()
        }
    }

    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
        logger.info(TAG, "onAttendeesJoined")
        onAttendeesJoinedWithStatus(attendeeInfo)
    }

    private fun isSelfAttendee(attendeeId: String): Boolean {
        return DefaultModality(attendeeId).base() == meetingCredentials.attendeeId
    }

    private fun getAttendeeName(attendeeId: String, externalUserId: String): String {
        if ((attendeeId.isEmpty() || externalUserId.isEmpty())) {
            return "<UNKNOWN>"
        }
        val attendeeName = if (externalUserId.contains('#')) externalUserId.split('#')[1] else externalUserId

        return if (attendeeId.isContentShare()) {
            "$attendeeName $CONTENT_NAME_SUFFIX"
        } else {
            attendeeName
        }
    }

    private fun onAttendeesJoinedWithStatus(
        attendeeInfo: Array<AttendeeInfo>
    ) {
        uiScope.launch {
                attendeeInfo.forEach { (attendeeId, externalUserId) ->
                    meetingModel.currentRoster.getOrPut(
                        attendeeId
                    ) {
                        RosterAttendee(
                            attendeeId,
                            getAttendeeName(attendeeId, externalUserId),
                            attendeeStatus = AttendeeStatus.Joined
                        )
                    }
                    rosterAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        uiScope.launch {
            attendeeInfo.forEach { (attendeeId, _) ->
                meetingModel.currentRoster.remove(
                    attendeeId
                )
            }

            rosterAdapter.notifyDataSetChanged()
        }
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        attendeeInfo.forEach { (attendeeId, externalUserId) ->
            logger.info(TAG,
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

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        uiScope.launch {
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
        }
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        uiScope.launch {
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

    override fun onVideoTileAdded(tileState: VideoTileState) {
        uiScope.launch {
            logger.info(
                TAG,
                "Video tile added, tileId: ${tileState.tileId}, attendeeId: ${tileState.attendeeId}" +
                        ", isContent ${tileState.isContent} with size ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight}"
            )
            showVideoTile(tileState)
        }
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        val attendeeName = meetingModel.currentRoster[attendeeId]?.attendeeName ?: ""
        return VideoCollectionTile(attendeeName, tileState)
    }

    private fun showVideoTile(tileState: VideoTileState) {
        val videoCollectionTile = createVideoCollectionTile(tileState)
        meetingModel.videos.add(videoCollectionTile)
        videoAdapter.notifyDataSetChanged()
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        if (tileState.pauseState == VideoPauseState.PausedForPoorConnection) {
            val collection = meetingModel.videos
            collection.find { it.videoTileState.tileId == tileState.tileId }.apply {
                this?.setPauseMessageVisibility(View.VISIBLE)
            }
            val attendeeName =
                meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
            logger.info(TAG,
                "$attendeeName video paused"
            )
        }
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        uiScope.launch {
            val tileId: Int = tileState.tileId

            logger.info(
                TAG,
                "Video track removed, tileId: $tileId, attendeeId: ${tileState.attendeeId}"
            )
            meetingModel.unbindVideoView(tileId)
            meetingModel.videos.removeAll { it.videoTileState.tileId == tileId }
            videoAdapter.notifyDataSetChanged()
        }
    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        val collection = meetingModel.videos
        collection.find { it.videoTileState.tileId == tileState.tileId }.apply {
            this?.setPauseMessageVisibility(View.INVISIBLE)
        }
        val attendeeName = meetingModel.currentRoster[tileState.attendeeId]?.attendeeName ?: ""
        logger.info(TAG,
            "$attendeeName video resumed"
        )
    }

    override fun onVideoTileSizeChanged(tileState: VideoTileState) {
        logger.info(
            TAG,
            "Video stream content size changed to ${tileState.videoStreamContentWidth}*${tileState.videoStreamContentHeight} for tileId: ${tileState.tileId}"
        )
    }
}
