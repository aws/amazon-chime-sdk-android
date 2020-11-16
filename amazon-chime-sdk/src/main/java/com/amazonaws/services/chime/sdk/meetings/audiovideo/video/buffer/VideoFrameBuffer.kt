/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame

/**
 * [VideoFrameBuffer] is a buffer which contains a single video buffer's raw data.
 * Typically owned by a [VideoFrame] which includes additional metadata.
 */
interface VideoFrameBuffer {
    /**
     * Width of the video frame buffer
     */
    val width: Int

    /**
     * Height of the video frame buffer
     */
    val height: Int

    /**
     * Retain the video frame buffer. Use when shared ownership of the buffer
     * is desired (e.g. when passing to separate thread), otherwise the frame
     * may be spuriously released
     */
    fun retain()

    /**
     * Release the video frame buffer. Use after frame construction or [release]
     * after the frame is no longer needed. Will trigger appropriate release of
     * any internally allocated resources. Not using may result in leaks.
     */
    fun release()
}
