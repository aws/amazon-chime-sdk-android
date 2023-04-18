/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo.models

import androidx.lifecycle.ViewModel
import com.amazonaws.services.chime.minimaldemo.data.RosterAttendee
import com.amazonaws.services.chime.minimaldemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRenderView
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession
import java.util.concurrent.ConcurrentHashMap

class MeetingViewModel(val meetingSession: MeetingSession) : ViewModel() {
    var isLocalVideoOn: Boolean = false
        private set

    var isMuted: Boolean = false
        private set

    val currentRoster = ConcurrentHashMap<String, RosterAttendee>()
    val videos = mutableListOf<VideoCollectionTile>()

    private val userPausedVideoTileIds = mutableSetOf<Int>()

    fun toggleLocalVideo() {
        if (!isLocalVideoOn) {
            meetingSession.audioVideo.startLocalVideo()
        } else {
            meetingSession.audioVideo.stopLocalVideo()
        }
        isLocalVideoOn = !isLocalVideoOn
    }

    fun toggleMute() {
        if (isMuted) meetingSession.audioVideo.realtimeLocalUnmute()
        else meetingSession.audioVideo.realtimeLocalMute()

        isMuted = !isMuted
    }

    fun selectAudioDevice() {
        meetingSession.audioVideo.chooseAudioDevice(meetingSession.audioVideo.listAudioDevices().first())
    }

    fun enableVoiceFocus() {
        meetingSession.audioVideo.realtimeSetVoiceFocusEnabled(true)
    }

    fun bindVideoView(view: VideoRenderView, tileId: Int) {
        meetingSession.audioVideo.bindVideoView(view, tileId)
    }

    fun unbindVideoView(tileId: Int) {
        meetingSession.audioVideo.unbindVideoView(tileId)
    }

    fun switchCamera() {
        meetingSession.audioVideo.switchCamera()
    }

    fun pauseRemoteVideoTile(tileId: Int) {
        meetingSession.audioVideo.pauseRemoteVideoTile(tileId)
        userPausedVideoTileIds.add(tileId)
    }

    fun resumeRemoteVideoTile(tileId: Int) {
        meetingSession.audioVideo.resumeRemoteVideoTile(tileId)
        userPausedVideoTileIds.remove(tileId)
    }

    fun getActiveCamera(): MediaDevice? {
        return meetingSession.audioVideo.getActiveCamera()
    }

    fun endMeeting() {
        meetingSession.audioVideo.stopLocalVideo()
        meetingSession.audioVideo.stopContentShare()
        meetingSession.audioVideo.stopRemoteVideo()
        meetingSession.audioVideo.stop()
    }
}
