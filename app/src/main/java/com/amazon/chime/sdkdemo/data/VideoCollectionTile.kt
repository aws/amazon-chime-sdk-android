/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.chime.sdkdemo.data

import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileState

data class VideoCollectionTile(
    val attendeeName: String,
    val videoTileState: VideoTileState
)
