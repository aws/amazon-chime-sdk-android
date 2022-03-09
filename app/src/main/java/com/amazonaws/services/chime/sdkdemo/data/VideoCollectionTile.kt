/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.chime.sdkdemo.data

import androidx.constraintlayout.widget.ConstraintLayout
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState

data class VideoCollectionTile(
    val attendeeName: String
) {
    lateinit var videoTileState: VideoTileState
    var remoteVideoSource: RemoteVideoSource? = null
    var videoSubscriptionConfiguration: VideoSubscriptionConfiguration? = null

    var videoRenderView: DefaultVideoRenderView? = null
    var pauseMessageView: ConstraintLayout? = null

    fun setRenderViewVisibility(visibility: Int) {
        videoRenderView?.visibility = visibility
    }

    fun setPauseMessageVisibility(visibility: Int) {
        pauseMessageView?.visibility = visibility
    }

    fun setTileState(videoTileState: VideoTileState) {
        this.videoTileState = videoTileState
    }

    fun setVideoSource(remoteVideoSource: RemoteVideoSource) {
        this.remoteVideoSource = remoteVideoSource
    }

    fun setSubscriptionConfiguration(videoSubscriptionConfiguration: VideoSubscriptionConfiguration) {
        this.videoSubscriptionConfiguration = videoSubscriptionConfiguration
    }
}
