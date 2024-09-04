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
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPriority
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoScalingType
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSubscriptionConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.SegmentationProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur.BackgroundBlurVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement.BackgroundReplacementVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.databinding.ItemVideoBinding
import com.amazonaws.services.chime.sdkdemo.model.MeetingModel
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode

class VideoAdapter(
    private val videoCollectionTiles: Collection<VideoCollectionTile>,
    private val meetingModel: MeetingModel,
    private val audioVideoFacade: AudioVideoFacade,
    private val cameraCaptureSource: CameraCaptureSource?,
    private val backgroundBlurVideoFrameProcessor: BackgroundBlurVideoFrameProcessor?,
    private val backgroundReplacementVideoFrameProcessor: BackgroundReplacementVideoFrameProcessor?,
    private val context: Context?,
    private val logger: Logger
) : RecyclerView.Adapter<VideoHolder>() {
    private lateinit var tabContentLayout: ConstraintLayout
    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        tabContentLayout = (context as MeetingActivity).findViewById(R.id.constraintLayout)
        val inflatedView = parent.inflate(R.layout.item_video, false)
        return VideoHolder(
            context,
            inflatedView,
            audioVideoFacade,
            meetingModel,
            logger,
            cameraCaptureSource,
            backgroundBlurVideoFrameProcessor,
            backgroundReplacementVideoFrameProcessor
        )
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
                    holder.tileContainer.layoutParams.height =
                        (VIDEO_ASPECT_RATIO_16_9 * viewportWidth).toInt()
                }
            }
            val videoRenderView = holder.binding.videoSurface
            videoRenderView.scalingType = VideoScalingType.AspectFit
            videoRenderView.hardwareScaling = false
        }
    }
}

class VideoHolder(
    private val context: Context,
    private val view: View,
    private val audioVideo: AudioVideoFacade,
    private val meetingModel: MeetingModel,
    private val logger: Logger,
    private val cameraCaptureSource: CameraCaptureSource?,
    private val backgroundBlurVideoFrameProcessor: BackgroundBlurVideoFrameProcessor?,
    private val backgroundReplacementVideoFrameProcessor: BackgroundReplacementVideoFrameProcessor?,
    val binding: ItemVideoBinding = ItemVideoBinding.bind(view)
) : RecyclerView.ViewHolder(view) {

    val tileContainer: ConstraintLayout = view.findViewById(R.id.tile_container)
    private val TAG = "VideoHolder"

    init {
        binding.videoSurface.logger = logger
    }

    fun bindVideoTile(videoCollectionTile: VideoCollectionTile) {
        audioVideo.bindVideoView(binding.videoSurface, videoCollectionTile.videoTileState.tileId)
        // Save the bound VideoRenderView in order to explicitly control the visibility of SurfaceView.
        // This is to bypass the issue where we cannot hide a SurfaceView that overlaps with another one.
        videoCollectionTile.videoRenderView = binding.videoSurface
        videoCollectionTile.pauseMessageView = binding.poorConnectionMessage

        if (videoCollectionTile.videoTileState.isContent) {
            binding.videoSurface.contentDescription = "ScreenTile"
        } else {
            binding.videoSurface.contentDescription = "${videoCollectionTile.attendeeName} VideoTile"
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            binding.onTileButton.setImageResource(R.drawable.ic_switch_camera)
            binding.attendeeName.visibility = View.GONE
            binding.onTileButton.visibility = View.VISIBLE

            if (SegmentationProcessor.isMachineLearningLibraryLoaded) {
                binding.videoFilterButton.setImageResource(R.drawable.button_more)
                binding.videoFilterButton.visibility = View.VISIBLE
            } else {
                binding.videoFilterButton.visibility = View.INVISIBLE
            }
            // To facilitate demoing and testing both use cases, we account for both our external
            // camera and the camera managed by the facade. Actual applications should
            // only use one or the other
            updateLocalVideoMirror()
            binding.onTileButton.setOnClickListener {
                if (audioVideo.getActiveCamera() != null) {
                    audioVideo.switchCamera()
                } else {
                    cameraCaptureSource?.switchCamera()
                }
                updateLocalVideoMirror()
            }
            if (SegmentationProcessor.isMachineLearningLibraryLoaded) {
                binding.videoFilterButton.setOnClickListener {
                    showVideoFilterPopup(binding.videoFilterButton)
                }
            }
        } else {
            binding.videoSurface.mirror = false
            binding.attendeeName.text = videoCollectionTile.attendeeName
            binding.attendeeName.visibility = View.VISIBLE
            binding.onTileButton.visibility = View.VISIBLE
            binding.videoFilterButton.visibility = View.INVISIBLE
            when (videoCollectionTile.videoTileState.pauseState) {
                VideoPauseState.Unpaused ->
                    binding.onTileButton.setImageResource(R.drawable.ic_pause_video)
                VideoPauseState.PausedByUserRequest ->
                    binding.onTileButton.setImageResource(R.drawable.ic_resume_video)
                VideoPauseState.PausedForPoorConnection ->
                    binding.poorConnectionMessage.visibility = View.VISIBLE
            }

            binding.onTileButton.setOnClickListener {
                val tileId = videoCollectionTile.videoTileState.tileId
                if (videoCollectionTile.videoTileState.pauseState == VideoPauseState.Unpaused) {
                    audioVideo.pauseRemoteVideoTile(tileId)
                    meetingModel.userPausedVideoTileIds.add(tileId)
                    binding.onTileButton.setImageResource(R.drawable.ic_resume_video)
                } else {
                    audioVideo.resumeRemoteVideoTile(tileId)
                    meetingModel.userPausedVideoTileIds.remove(tileId)
                    binding.onTileButton.setImageResource(R.drawable.ic_pause_video)
                }
            }

            binding.videoSurface.setOnClickListener {
                val attendeeId = videoCollectionTile.videoTileState.attendeeId
                showPriorityPopup(binding.onTileButton, attendeeId)
            }
        }

        binding.videoConfigButton.setOnClickListener {
            val attendeeId = videoCollectionTile.videoTileState.attendeeId
            showResolutionPopup(binding.videoConfigButton, attendeeId)
        }
    }

    private fun showPriorityPopup(view: View, attendeeId: String) {
        val popup = PopupMenu(context, view)
        popup.inflate(R.menu.priority_popup_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            val priority = when (item?.itemId) {
                R.id.highest -> {
                    VideoPriority.Highest
                }
                R.id.high -> {
                    VideoPriority.High
                }
                R.id.medium -> {
                    VideoPriority.Medium
                }
                R.id.low -> {
                    VideoPriority.Low
                }
                R.id.lowest -> {
                    VideoPriority.Lowest
                }
                else -> {
                    VideoPriority.Lowest
                }
            }

            val updatedSource = mutableMapOf<RemoteVideoSource, VideoSubscriptionConfiguration>()
            for (source in meetingModel.getRemoteVideoSourceConfigurations()) {
                if (source.key.attendeeId == attendeeId) {
                    val resolution: VideoResolution = source.value.targetResolution
                    val config = VideoSubscriptionConfiguration(priority, resolution)
                    meetingModel.setRemoteVideoSourceConfigurations(source.key, config)
                    updatedSource[source.key] = source.value
                }
            }
            audioVideo.updateVideoSourceSubscriptions(
                meetingModel.getRemoteVideoSourceConfigurations(),
                emptyArray()
            )
            true
        }
        popup.show()
    }

    private fun showResolutionPopup(view: View, attendeeId: String) {
        val popup = PopupMenu(context, view)
        popup.inflate(R.menu.resolution_popup_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            val resolution = when (item?.itemId) {
                R.id.high -> {
                    VideoResolution.High
                }
                R.id.medium -> {
                    VideoResolution.Medium
                }
                R.id.low -> {
                    VideoResolution.Low
                }
                else -> {
                    VideoResolution.Low
                }
            }

            val updatedSource = mutableMapOf<RemoteVideoSource, VideoSubscriptionConfiguration>()
            for (source in meetingModel.getRemoteVideoSourceConfigurations()) {
                if (source.key.attendeeId == attendeeId) {
                    val priority: VideoPriority = source.value.priority
                    val config = VideoSubscriptionConfiguration(priority, resolution)
                    meetingModel.setRemoteVideoSourceConfigurations(source.key, config)
                    updatedSource[source.key] = source.value
                }
            }

            audioVideo.updateVideoSourceSubscriptions(
                updatedSource,
                emptyArray()
            )
            true
        }
        popup.show()
    }

    private fun showVideoFilterPopup(view: View) {
        val popup = PopupMenu(context, view)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                R.id.backgroundBlur -> {
                    toggleVideoBackgroundBlur()
                }
                R.id.backgroundReplacement -> {
                    toggleVideoBackgroundReplacement()
                }
                else -> {}
            }
            true
        }
        popup.inflate(R.menu.video_filter_popup_menu)
        if (meetingModel.isUsingBackgroundBlur) {
            popup.menu.getItem(0).setTitle(R.string.disable_background_blur)
        }
        if (meetingModel.isUsingBackgroundReplacement) {
            popup.menu.getItem(1).setTitle(R.string.disable_background_replacement)
        }
        popup.show()
    }

    private fun toggleVideoBackgroundBlur() {
        if (!meetingModel.isUsingCameraCaptureSource) {
            logger.warn(TAG, "Cannot toggle filter without using custom camera capture source.")
            Toast.makeText(
                context,
                R.string.user_notification_filter_custom_source_error,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (meetingModel.isUsingBackgroundReplacement || meetingModel.isUsingCpuVideoProcessor || meetingModel.isUsingGpuVideoProcessor) {
            logger.warn(
                TAG,
                "Cannot toggle background blur filter when other filter is enabled."
            )
            Toast.makeText(
                context,
                R.string.user_notification_background_blur_custom_source_error,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        logger.info(
            TAG,
            "Toggling Background Blur from ${meetingModel.isUsingBackgroundBlur} to ${!meetingModel.isUsingBackgroundBlur}."
        )
        meetingModel.isUsingBackgroundBlur = !meetingModel.isUsingBackgroundBlur
        if (meetingModel.isLocalVideoStarted) {
            startLocalVideo()
        }
    }

    private fun toggleVideoBackgroundReplacement() {
        if (!meetingModel.isUsingCameraCaptureSource) {
            logger.warn(TAG, "Cannot toggle filter without using custom camera capture source.")
            Toast.makeText(
                context,
                R.string.user_notification_filter_custom_source_error,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (meetingModel.isUsingBackgroundBlur || meetingModel.isUsingCpuVideoProcessor || meetingModel.isUsingGpuVideoProcessor) {
            logger.warn(
                TAG,
                "Cannot toggle background replacement filter when other filter is enabled."
            )
            Toast.makeText(
                context,
                R.string.user_notification_background_replacement_custom_source_error,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        logger.info(
            TAG,
            "Toggling Background Replacement from ${meetingModel.isUsingBackgroundReplacement} to ${!meetingModel.isUsingBackgroundReplacement}."
        )
        meetingModel.isUsingBackgroundReplacement = !meetingModel.isUsingBackgroundReplacement
        if (meetingModel.isLocalVideoStarted) {
            startLocalVideo()
        }
    }

    private fun startLocalVideo() {
        meetingModel.isLocalVideoStarted = true
        if (meetingModel.isUsingCameraCaptureSource) {
            when {
                meetingModel.isUsingBackgroundBlur -> {
                    if (backgroundBlurVideoFrameProcessor != null) {
                        cameraCaptureSource?.addVideoSink(backgroundBlurVideoFrameProcessor)
                        audioVideo.startLocalVideo(backgroundBlurVideoFrameProcessor)
                    }
                }
                meetingModel.isUsingBackgroundReplacement -> {
                    if (backgroundReplacementVideoFrameProcessor != null) {
                        cameraCaptureSource?.addVideoSink(backgroundReplacementVideoFrameProcessor)
                        audioVideo.startLocalVideo(backgroundReplacementVideoFrameProcessor)
                    }
                }
                else -> {
                    if (cameraCaptureSource != null) {
                        audioVideo.startLocalVideo(cameraCaptureSource)
                    }
                }
            }
            cameraCaptureSource?.start()
        } else {
            audioVideo.startLocalVideo()
        }
    }

    private fun updateLocalVideoMirror() {
        binding.videoSurface.mirror =
                // If we are using internal source, base mirror state off that device type
            (audioVideo.getActiveCamera()?.type == MediaDeviceType.VIDEO_FRONT_CAMERA ||
                    // Otherwise (audioVideo.getActiveCamera() == null) use the device type of our external/custom camera capture source
                    (audioVideo.getActiveCamera() == null && cameraCaptureSource?.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA))
    }
}
