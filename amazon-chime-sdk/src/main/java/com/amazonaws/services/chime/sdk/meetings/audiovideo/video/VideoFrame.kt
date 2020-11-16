/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSource

/**
 * [VideoFrame] is a class which contains a [VideoFrameBuffer] and metadata necessary for transmission
 * Typically produced via a [VideoSource] and consumed via a [VideoSink]
 */
class VideoFrame(
    /**
     * Timestamp in nanoseconds at which the video frame was captured from some system monotonic clock.
     * Will be aligned and converted to NTP (Network Time Protocol) within AmazonChimeSDKMedia library, which will then
     * be converted to a system monotonic clock on remote end. May be different on frames emanated from AmazonChimeSDKMedia library.
     *
     * See [DefaultSurfaceTextureCaptureSource] for usage of a AmazonChimeSDKMedia library class which can convert to the clock used by the library
     */
    val timestampNs: Long,

    /**
     * Object containing actual video frame data in some form
     */
    val buffer: VideoFrameBuffer,

    /**
     * Rotation of the video frame buffer in degrees clockwise
     * from intended viewing horizon.
     *
     * e.g. If you were recording camera capture upside down relative to
     * the orientation of the sensor, this value would be [VideoRotation.Rotation180].
     */
    val rotation: VideoRotation = VideoRotation.Rotation0
) {
    /**
     * Width of the video frame
     *
     * @return [Int] - Frame width in pixels
     */
    val width: Int
        get() = buffer.width

    /**
     * Height of the video frame
     *
     * @return [Int] - Frame height in pixels
     */
    val height: Int
        get() = buffer.height

    /**
     * Width of frame when the reverse of [rotation] is applied to the buffer
     * e.g. a frame with width = 1 and height = 2 with 90 degrees
     * rotation will have rotated width = 2 and height = 1
     *
     * @return [Int] - Frame width when rotation is removed
     */
    fun getRotatedWidth(): Int {
        return if (rotation.degrees % 180 == 0) {
            buffer.width
        } else {
            buffer.height
        }
    }

    /**
     * Height of frame when the reverse of [rotation] is applied to the buffer
     * e.g. a frame with width = 1 and height = 2 with 90 degrees
     * rotation will have rotated width = 2 and height = 1
     *
     * @return [Int] - Frame height when rotation is removed
     */
    fun getRotatedHeight(): Int {
        return if (rotation.degrees % 180 == 0) {
            buffer.height
        } else {
            buffer.width
        }
    }

    /**
     * Helper function to call [VideoFrameBuffer.retain] on the owned buffer
     */
    fun retain() = buffer.retain()

    /**
     * Helper function to call [VideoFrameBuffer.release] on the owned buffer
     */
    fun release() = buffer.release()
}
