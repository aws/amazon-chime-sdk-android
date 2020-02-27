package com.amazon.chime.sdkdemo.data

import com.amazon.chime.sdk.media.mediacontroller.video.VideoTile

data class VideoCollectionTile(
    val attendeeName: String,
    val isLocal: Boolean,
    val videoTile: VideoTile
)
