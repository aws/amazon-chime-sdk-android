/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.minimaldemo.R
import com.amazonaws.services.chime.minimaldemo.data.VideoCollectionTile
import com.amazonaws.services.chime.minimaldemo.models.MeetingViewModel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoScalingType
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class VideoAdapter(
    private val videoCollectionTiles: Collection<VideoCollectionTile>,
    private val meetingModel: MeetingViewModel,
    private val logger: Logger
) : RecyclerView.Adapter<VideoHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoHolder(
            inflatedView,
            meetingModel
        )
    }

    override fun getItemCount(): Int {
        return videoCollectionTiles.size
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        val videoCollectionTile = videoCollectionTiles.elementAt(position)
        holder.bindVideoTile(videoCollectionTile)
        holder.videoSurface.scalingType = VideoScalingType.AspectFit
    }
}

class VideoHolder(
    private val view: View,
    private val meetingModel: MeetingViewModel
) : RecyclerView.ViewHolder(view) {

    val tileContainer: ConstraintLayout = view.findViewById(R.id.tile_container)
    private val TAG = "VideoHolder"
    val videoSurface: DefaultVideoRenderView = view.findViewById(R.id.video_surface)
    private val poorConnectionMessage: ConstraintLayout = view.findViewById(R.id.poor_connection_message)
    private val onTileButton: ImageButton = view.findViewById(R.id.on_tile_button)
    private val attendeeName: TextView = view.findViewById(R.id.video_attendee_name)

    fun bindVideoTile(videoCollectionTile: VideoCollectionTile) {
        meetingModel.bindVideoView(videoSurface, videoCollectionTile.videoTileState.tileId)
        // Save the bound VideoRenderView in order to explicitly control the visibility of SurfaceView.
        // This is to bypass the issue where we cannot hide a SurfaceView that overlaps with another one.
        videoCollectionTile.videoRenderView = videoSurface
        videoCollectionTile.pauseMessageView = poorConnectionMessage

        if (videoCollectionTile.videoTileState.isContent) {
            videoSurface.contentDescription = "ScreenTile"
        } else {
            videoSurface.contentDescription = "${videoCollectionTile.attendeeName} VideoTile"
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            onTileButton.setImageResource(R.drawable.ic_switch_camera)
            attendeeName.visibility = View.GONE
            onTileButton.visibility = View.VISIBLE

            // To facilitate demoing and testing both use cases, we account for both our external
            // camera and the camera managed by the facade. Actual applications should
            // only use one or the other
            updateLocalVideoMirror()
            onTileButton.setOnClickListener {
                meetingModel.switchCamera()
                updateLocalVideoMirror()
            }
        } else {
            videoSurface.mirror = false
            attendeeName.text = videoCollectionTile.attendeeName
            attendeeName.visibility = View.VISIBLE
            onTileButton.visibility = View.VISIBLE
            when (videoCollectionTile.videoTileState.pauseState) {
                VideoPauseState.Unpaused ->
                    onTileButton.setImageResource(R.drawable.ic_pause_video)
                VideoPauseState.PausedByUserRequest ->
                    onTileButton.setImageResource(R.drawable.ic_resume_video)
                VideoPauseState.PausedForPoorConnection ->
                    poorConnectionMessage.visibility = View.VISIBLE
            }

            onTileButton.setOnClickListener {
                val tileId = videoCollectionTile.videoTileState.tileId
                if (videoCollectionTile.videoTileState.pauseState == VideoPauseState.Unpaused) {
                    meetingModel.pauseRemoteVideoTile(tileId)
                    onTileButton.setImageResource(R.drawable.ic_resume_video)
                } else {
                    meetingModel.resumeRemoteVideoTile(tileId)
                    onTileButton.setImageResource(R.drawable.ic_pause_video)
                }
            }
        }
    }

    private fun updateLocalVideoMirror() {
        videoSurface.mirror = (meetingModel.getActiveCamera()?.type == MediaDeviceType.VIDEO_FRONT_CAMERA)
    }
}
