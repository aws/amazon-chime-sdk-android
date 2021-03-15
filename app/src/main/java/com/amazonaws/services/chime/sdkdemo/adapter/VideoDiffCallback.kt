package com.amazonaws.services.chime.sdkdemo.adapter

import androidx.recyclerview.widget.DiffUtil
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile

class VideoDiffCallback(
    private val oldList: List<VideoCollectionTile>,
    private val newList: List<VideoCollectionTile>
) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].videoTileState.attendeeId == newList[newItemPosition].videoTileState.attendeeId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldTileState = oldList[oldItemPosition].videoTileState
        val newTileState = newList[newItemPosition].videoTileState

        return oldTileState.attendeeId == newTileState.attendeeId &&
                oldTileState.pauseState == newTileState.pauseState &&
                oldTileState.videoStreamContentWidth == newTileState.videoStreamContentWidth &&
                oldTileState.videoStreamContentHeight == newTileState.videoStreamContentHeight
    }
}
