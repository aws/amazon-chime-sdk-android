/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import kotlinx.android.synthetic.main.item_video.view.attendee_name
import kotlinx.android.synthetic.main.item_video.view.on_tile_button
import kotlinx.android.synthetic.main.item_video.view.video_surface

class VideoAdapter(
    private val videoCollectionTiles: Collection<VideoCollectionTile>,
    private val audioVideoFacade: AudioVideoFacade,
    private val context: Context?
) :
    RecyclerView.Adapter<VideoHolder>() {
    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val inflatedView = parent.inflate(R.layout.item_video, false)
        return VideoHolder(inflatedView, audioVideoFacade)
    }

    override fun getItemCount(): Int {
        return videoCollectionTiles.size
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        val videoCollectionTile = videoCollectionTiles.elementAt(position)
        holder.bindVideoTile(videoCollectionTile)
        context?.let {
            val displayMetrics = context.resources.displayMetrics
            val width =
                if (isLandscapeMode(context) == true) displayMetrics.widthPixels / 2 else displayMetrics.widthPixels
            val height = (width * VIDEO_ASPECT_RATIO_16_9).toInt()
            holder.tileContainer.layoutParams.height = height
            holder.tileContainer.layoutParams.width = width.toInt()
        }
    }
}

class VideoHolder(inflatedView: View, audioVideoFacade: AudioVideoFacade) :
    RecyclerView.ViewHolder(inflatedView) {

    private var view: View = inflatedView
    private var audioVideo = audioVideoFacade
    val tileContainer: RelativeLayout = view.findViewById(R.id.tile_container)

    fun bindVideoTile(videoCollectionTile: VideoCollectionTile) {
        audioVideo.bindVideoView(view.video_surface, videoCollectionTile.videoTileState.tileId)
        if (videoCollectionTile.videoTileState.isContent) {
            view.video_surface.contentDescription = "ScreenTile"
        } else {
            view.video_surface.contentDescription = "${videoCollectionTile.attendeeName} VideoTile"
        }
        if (videoCollectionTile.videoTileState.isLocalTile) {
            view.attendee_name.visibility = View.GONE
            view.on_tile_button.visibility = View.VISIBLE
            view.on_tile_button.setOnClickListener { audioVideo.switchCamera() }
        } else {
            view.attendee_name.text = videoCollectionTile.attendeeName
            view.attendee_name.visibility = View.VISIBLE
            view.on_tile_button.visibility = View.VISIBLE
            if (videoCollectionTile.videoTileState.pauseState == VideoPauseState.Unpaused) {
                view.on_tile_button.setImageResource(R.drawable.ic_pause_video)
            } else {
                view.on_tile_button.setImageResource(R.drawable.ic_resume_video)
            }

            view.on_tile_button.setOnClickListener {
                if (videoCollectionTile.videoTileState.pauseState == VideoPauseState.Unpaused) {
                    audioVideo.pauseRemoteVideoTile(videoCollectionTile.videoTileState.tileId)
                    view.on_tile_button.setImageResource(R.drawable.ic_resume_video)
                } else {
                    audioVideo.resumeRemoteVideoTile(videoCollectionTile.videoTileState.tileId)
                    view.on_tile_button.setImageResource(R.drawable.ic_pause_video)
                }
            }
        }
    }
}
