/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [ContentShareVideoClientController] uses a separate video client for content share video related
 * functionality.
 */
interface ContentShareVideoClientController {
    /**
     * Start to share video with a provided custom [VideoSource] which can be used to provide custom
     * [VideoFrame]s to be transmitted to remote clients. This will call [VideoSource.addVideoSink]
     * on the provided source and [VideoSource.removeVideoSink] on the previously provided source.
     *
     * Calling this function repeatedly will replace the previous [VideoSource] as the one being
     * transmitted.
     *
     * @param videoSource: [VideoSource] - The source of video frames to be sent to other clients.
     */
    fun startVideoShare(videoSource: VideoSource)

    /**
     * Stop sending video to remote clients.
     */
    fun stopVideoShare()

    /**
     * Subscribe to video client state and connection events with an [ContentShareObserver]
     *
     * @param observer: [ContentShareObserver] - The observer to be notified for events.
     */
    fun subscribeToVideoClientStateChange(observer: ContentShareObserver)

    /**
     * Unsubscribe from video client state and connection events by removing the specified [ContentShareObserver]
     *
     * @param observer: [ContentShareObserver] - The observer to be removed for events.
     */
    fun unsubscribeFromVideoClientStateChange(observer: ContentShareObserver)
}
