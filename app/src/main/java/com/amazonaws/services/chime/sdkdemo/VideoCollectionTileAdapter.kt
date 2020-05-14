/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazonaws.services.chime.sdkdemo

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile
import kotlinx.android.synthetic.main.video_collection_item.view.attendee_name
import kotlinx.android.synthetic.main.video_collection_item.view.on_tile_button
import kotlinx.android.synthetic.main.video_collection_item.view.video_surface

class VideoCollectionTileAdapter(
    private val videoCollectionTiles: MutableCollection<VideoCollectionTile>,
    private val audioVideoFacade: AudioVideoFacade,
    private val context: Context?
) :
    RecyclerView.Adapter<ViewHolder>() {
    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflatedView = parent.inflate(R.layout.video_collection_item, false)
        return ViewHolder(inflatedView, audioVideoFacade)
    }

    override fun getItemCount(): Int {
        return videoCollectionTiles.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoCollectionTile = videoCollectionTiles.elementAt(position)
        holder.bindVideoTile(videoCollectionTile)
        context?.let {
            val displayMetrics = context.resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = (width * VIDEO_ASPECT_RATIO_16_9).toInt()
            holder.tileContainer.layoutParams.height = height
            holder.tileContainer.layoutParams.width = width.toInt()
        }
    }
}

class ViewHolder(inflatedView: View, audioVideoFacade: AudioVideoFacade) :
    RecyclerView.ViewHolder(inflatedView) {

    private var view: View = inflatedView
    private var audioVideo = audioVideoFacade
    val tileContainer: RelativeLayout = view.findViewById(R.id.tile_container)

    init {
        setIsRecyclable(false)
    }

    fun bindVideoTile(videoCollectionTile: VideoCollectionTile) {
        audioVideo.bindVideoView(view.video_surface, videoCollectionTile.videoTileState.tileId)
        view.video_surface.contentDescription = "${videoCollectionTile.attendeeName} VideoTile"
        if (videoCollectionTile.videoTileState.isLocalTile) {
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
