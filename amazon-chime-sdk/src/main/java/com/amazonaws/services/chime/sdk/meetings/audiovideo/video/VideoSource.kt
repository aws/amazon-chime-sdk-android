/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade

/**
 * [VideoSource] is an interface for sources which produce video frames, and can send to a [VideoSink].
 * Implementations can be passed to the [AudioVideoFacade] to be used as the video source sent to remote
 * participants
 */
interface VideoSource {
    /**
     * Add a video sink which will immediately begin to receive new frames.
     *
     * Multiple sinks can be added to a single [VideoSource] to allow forking of video frames,
     * e.g. to send to both local preview and AmazonChimeSDKMedia library (for encoding) at the same time.
     *
     * @param sink: [VideoSink] - New video sink
     */
    fun addVideoSink(sink: VideoSink)

    /**
     * Remove a video sink which will no longer receive new frames on return
     *
     * @param sink: [VideoSink] - Video sink to remove
     */
    fun removeVideoSink(sink: VideoSink)

    /**
     * Content hint for downstream processing
     */
    val contentHint: VideoContentHint
}
