/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.*
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
    private val remoteVideoSourceConfigurations: MutableMap<RemoteVideoSource, VideoSubscriptionConfiguration>,
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
        return VideoHolder(context, inflatedView, audioVideoFacade, userPausedVideoTileIds, remoteVideoSourceConfigurations, logger, cameraCaptureSource)
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
    private val context: Context,
    private val view: View,
    private val audioVideo: AudioVideoFacade,
    private val userPausedVideoTileIds: MutableSet<Int>,
    private val remoteVideoSourceConfigurations: MutableMap<RemoteVideoSource, VideoSubscriptionConfiguration>,
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

            view.video_surface.setOnClickListener {
                logger.info("VideoAdapter", "onClick hhh")
                val attendeeId = videoCollectionTile.videoTileState.attendeeId
                logger.info("VideoAdapter", attendeeId)
                val videoSource = RemoteVideoSource(attendeeId)
                remoteVideoSourceConfigurations.remove(videoSource)
                if(!remoteVideoSourceConfigurations.containsKey(videoSource)) {
                    remoteVideoSourceConfigurations[videoSource] =
                        VideoSubscriptionConfiguration(VideoPriority.HIGHEST, VideoResolution.LOW)
                    logger.info("VideoAdapter not contains:", videoSource.attendeeId)

                }
                val updatedConfig = remoteVideoSourceConfigurations[videoSource]
                logger.info("VideoAdapter", remoteVideoSourceConfigurations.size.toString())
                if (updatedConfig != null) {
                    showPopup(view.on_tile_button, videoSource, updatedConfig)
                }
                true

            }
        }
    }

    private fun showPopup(view: View, videoSource: RemoteVideoSource, updatedConfig: VideoSubscriptionConfiguration) {
        logger.info("VideoAdapter hhhh", "showPopUp")
        val popup = PopupMenu(context, view)
        popup.inflate(R.menu.popup_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.highest -> {
                    Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                    updatedConfig.priority = VideoPriority.HIGHEST
                }
                R.id.high -> {
                    Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                    updatedConfig.priority = VideoPriority.HIGH
                }
                R.id.medium -> {
                    Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                    updatedConfig.priority = VideoPriority.MEDIUM
                }
                R.id.low -> {
                    Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                    updatedConfig.priority = VideoPriority.LOW
                }
                R.id.lowest -> {
                    Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                    updatedConfig.priority = VideoPriority.LOWEST
                }
            }

            remoteVideoSourceConfigurations.remove(videoSource)
            remoteVideoSourceConfigurations.keys.forEach{
                logger.info("VideoAdapter before adding", it.attendeeId)
                logger.info("VideoAdapter before adding", remoteVideoSourceConfigurations[it]?.priority.toString())

            }
            remoteVideoSourceConfigurations[videoSource] = updatedConfig
            remoteVideoSourceConfigurations.keys.forEach{
                logger.info("VideoAdapter update", it.attendeeId)
                logger.info("VideoAdapter update", remoteVideoSourceConfigurations[it]?.priority.toString())

            }
            audioVideo.updateVideoSourceSubscriptions(remoteVideoSourceConfigurations, emptyArray())
            logger.info("VideoAdapter","attendeeId:")
            logger.info("VideoAdapter", videoSource.attendeeId)
            logger.info("VideoAdapter", updatedConfig.priority.toString())
            logger.info("VideoAdapter","remoteVideoSourceConfig size:")
            logger.info("VideoAdapter",remoteVideoSourceConfigurations.size.toString())
            true
        }
        popup.show();
    }

    private fun updateLocalVideoMirror() {
        view.video_surface.mirror =
            // If we are using internal source, base mirror state off that device type
            (audioVideo.getActiveCamera()?.type == MediaDeviceType.VIDEO_FRONT_CAMERA ||
            // Otherwise (audioVideo.getActiveCamera() == null) use the device type of our external/custom camera capture source
            (audioVideo.getActiveCamera() == null && cameraCaptureSource?.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA))
    }
}
