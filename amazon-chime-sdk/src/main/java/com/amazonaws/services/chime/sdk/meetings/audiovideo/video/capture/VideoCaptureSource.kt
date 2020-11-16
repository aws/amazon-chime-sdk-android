/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [VideoCaptureSource] is an interface for various video capture sources (i.e. screen, camera, file) which can emit [VideoFrame] objects
 * All the APIs here can be called regardless of whether the [AudioVideoFacade] is started or not.
 */
interface VideoCaptureSource :
    VideoSource {
    /**
     * Start capturing on this source and emitting video frames
     */
    fun start()

    /**
     * Stop capturing on this source and cease emitting video frames
     */
    fun stop()

    /**
     * Add a capture source observer to receive callbacks from the source on lifecycle events
     * which can be used to trigger UI. This observer is entirely optional.
     *
     * @param observer: [CaptureSourceObserver] - New observer
     */
    fun addCaptureSourceObserver(observer: CaptureSourceObserver)

    /**
     * Remove a capture source observer
     *
     * @param observer: [CaptureSourceObserver] - Observer to remove
     */
    fun removeCaptureSourceObserver(observer: CaptureSourceObserver)
}
