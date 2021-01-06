/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoScalingType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import kotlinx.android.synthetic.main.item_video.view.attendee_name
import kotlinx.android.synthetic.main.item_video.view.on_tile_button
import kotlinx.android.synthetic.main.item_video.view.poor_connection_message
import kotlinx.android.synthetic.main.item_video.view.video_surface

class VideoAdapter(
    private val videoCollectionTiles: Collection<VideoCollectionTile>,
    private val userPausedVideoTileIds: MutableSet<Int>,
    private val audioVideoFacade: AudioVideoFacade,
    private val cameraCaptureSource: CameraCaptureSource?,
    private val context: Context?,
    private val logger: Logger
) : RecyclerView.Adapter<VideoHolder>() {
    private lateinit var tabContentLayout: ConstraintLayout
    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        tabContentLayout = (context as MeetingActivity).findViewById(R.id.constraintLayout)
        val inflatedView = parent.inflate(R.layout.item_video, false)
        return VideoHolder(inflatedView, audioVideoFacade, userPausedVideoTileIds, logger, cameraCaptureSource)
    }

    override fun getItemCount(): Int {
        return videoCollectionTiles.size
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        val videoCollectionTile = videoCollectionTiles.elementAt(position)
        holder.bindVideoTile(videoCollectionTile)
        context?.let {
            if (!videoCollectionTile.videoTileState.isContent) {
                val viewportWidth = tabContentLayout.width
                if (isLandscapeMode(context)) {
                    holder.tileContainer.layoutParams.width = viewportWidth / 2
                } else {
                    holder.tileContainer.layoutParams.height = (VIDEO_ASPECT_RATIO_16_9 * viewportWidth).toInt()
                }
            }
            val videoRenderView = holder.itemView.video_surface
            videoRenderView.scalingType = VideoScalingType.AspectFit
            videoRenderView.hardwareScaling = false
        }
    }
}

class VideoHolder(
    private val view: View,
    private val audioVideo: AudioVideoFacade,
    private val userPausedVideoTileIds: MutableSet<Int>,
    private val logger: Logger,
    private val cameraCaptureSource: CameraCaptureSource?
) : RecyclerView.ViewHolder(view) {

    val tileContainer: ConstraintLayout = view.findViewById(R.id.tile_container)

    init {
        view.video_surface.logger = logger
    }

    fun bindVideoTile(videoCollectionTile: VideoCollectionTile) {
        audioVideo.bindVideoView(view.video_surface, videoCollectionTile.videoTileState.tileId)
        // Save the bound VideoRenderView in order to explicitly control the visibility of SurfaceView.
        // This is to bypass the issue where we cannot hide a SurfaceView that overlaps with another one.
        videoCollectionTile.videoRenderView = view.video_surface
        videoCollectionTile.pauseMessageView = view.poor_connection_message

        if (videoCollectionTile.videoTileState.isContent) {
            view.video_surface.contentDescription = "ScreenTile"
        } else {
            view.video_surface.contentDescription = "${videoCollectionTile.attendeeName} VideoTile"
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            view.on_tile_button.setImageResource(R.drawable.ic_switch_camera)
            view.attendee_name.visibility = View.GONE
            view.on_tile_button.visibility = View.VISIBLE

            // To facilitate demoing and testing both use cases, we account for both our external
            // camera and the camera managed by the facade. Actual applications should
            // only use one or the other
            updateLocalVideoMirror()
            view.on_tile_button.setOnClickListener {
                if (audioVideo.getActiveCamera() != null) {
                    audioVideo.switchCamera()
                } else {
                    cameraCaptureSource?.switchCamera()
                }
                updateLocalVideoMirror()
            }
        } else {
            view.video_surface.mirror = false
            view.attendee_name.text = videoCollectionTile.attendeeName
            view.attendee_name.visibility = View.VISIBLE
            view.on_tile_button.visibility = View.VISIBLE
            when (videoCollectionTile.videoTileState.pauseState) {
                VideoPauseState.Unpaused ->
                    view.on_tile_button.setImageResource(R.drawable.ic_pause_video)
                VideoPauseState.PausedByUserRequest ->
                    view.on_tile_button.setImageResource(R.drawable.ic_resume_video)
                VideoPauseState.PausedForPoorConnection ->
                    view.poor_connection_message.visibility = View.VISIBLE
            }

            view.on_tile_button.setOnClickListener {
                val tileId = videoCollectionTile.videoTileState.tileId
                if (videoCollectionTile.videoTileState.pauseState == VideoPauseState.Unpaused) {
                    audioVideo.pauseRemoteVideoTile(tileId)
                    userPausedVideoTileIds.add(tileId)
                    view.on_tile_button.setImageResource(R.drawable.ic_resume_video)
                } else {
                    audioVideo.resumeRemoteVideoTile(tileId)
                    userPausedVideoTileIds.remove(tileId)
                    view.on_tile_button.setImageResource(R.drawable.ic_pause_video)
                }
            }
        }
    }

    private fun updateLocalVideoMirror() {
        view.video_surface.mirror =
            // If we are using internal source, base mirror state off that device type
            (audioVideo.getActiveCamera()?.type == MediaDeviceType.VIDEO_FRONT_CAMERA ||
            // Otherwise (audioVideo.getActiveCamera() == null) use the device type of our external/custom camera capture source
            (audioVideo.getActiveCamera() == null && cameraCaptureSource?.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA))
    }
}
