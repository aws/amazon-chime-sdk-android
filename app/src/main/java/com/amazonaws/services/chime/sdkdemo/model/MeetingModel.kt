/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.ViewModel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import kotlin.math.ceil
import kotlin.math.min

// This will be used for keeping state after rotation
class MeetingModel : ViewModel() {
    val localTileId = 0
    private val videoTileCountPerPage = 4

    val currentMetrics = mutableMapOf<String, MetricData>()
    val currentRoster = mutableMapOf<String, RosterAttendee>()
    var localVideoTileState: VideoCollectionTile? = null
    val remoteVideoTileStates = mutableListOf<VideoCollectionTile>()
    val videoStatesInCurrentPage = mutableListOf<VideoCollectionTile>()
    val userPausedVideoTileIds = mutableSetOf<Int>()
    val currentScreenTiles = mutableListOf<VideoCollectionTile>()
    var currentVideoPageIndex = 0
    var currentMediaDevices = listOf<MediaDevice>()
    var currentMessages = mutableListOf<Message>()

    var isMuted = false
    var isCameraOn = false
    var isDeviceListDialogOn = false
    var isAdditionalOptionsDialogOn = false
    var isSharingContent = false
    var lastReceivedMessageTimestamp = 0L
    var tabIndex = 0
    var isUsingCameraCaptureSource = true
    var isLocalVideoStarted = false
    var wasLocalVideoStarted = false
    var isUsingGpuVideoProcessor = false
    var isUsingCpuVideoProcessor = false

    fun updateVideoStatesInCurrentPage() {
        videoStatesInCurrentPage.clear()

        if (localVideoTileState != null) {
            videoStatesInCurrentPage.add(localVideoTileState!!)
        }

        val remoteVideoTileCountPerPage = if (localVideoTileState == null) videoTileCountPerPage else (videoTileCountPerPage - 1)
        val remoteVideoStartIndex = currentVideoPageIndex * remoteVideoTileCountPerPage
        val remoteVideoEndIndex = min(remoteVideoTileStates.size, remoteVideoStartIndex + remoteVideoTileCountPerPage) - 1
        if (remoteVideoStartIndex <= remoteVideoEndIndex) {
            videoStatesInCurrentPage.addAll(remoteVideoTileStates.slice(remoteVideoStartIndex..remoteVideoEndIndex))
        }
    }

    fun updateRemoteVideoStatesBasedOnActiveSpeakers(activeSpeakers: Array<AttendeeInfo>) {
        val activeSpeakerIds = activeSpeakers.map { it.attendeeId }.toHashSet()
        remoteVideoTileStates.sortWith(Comparator<VideoCollectionTile> { lhs, rhs ->
            val lhsIsActiveSpeaker = activeSpeakerIds.contains(lhs.videoTileState.attendeeId)
            val rhsIsActiveSpeaker = activeSpeakerIds.contains(rhs.videoTileState.attendeeId)

            when {
                lhsIsActiveSpeaker && !rhsIsActiveSpeaker -> -1
                !lhsIsActiveSpeaker && rhsIsActiveSpeaker -> 1
                else -> 0
            }
        })
    }

    fun remoteVideoCountInCurrentPage(): Int {
        return videoStatesInCurrentPage.filter { !it.videoTileState.isLocalTile }.size
    }

    fun canGoToPrevVideoPage(): Boolean {
        return currentVideoPageIndex > 0
    }

    fun canGoToNextVideoPage(): Boolean {
        val remoteVideoTileCountPerPage = if (localVideoTileState == null) videoTileCountPerPage else (videoTileCountPerPage - 1)
        val maxVideoPageIndex = ceil(remoteVideoTileStates.size.toDouble() / remoteVideoTileCountPerPage).toInt() - 1
        return currentVideoPageIndex < maxVideoPageIndex
    }
}
