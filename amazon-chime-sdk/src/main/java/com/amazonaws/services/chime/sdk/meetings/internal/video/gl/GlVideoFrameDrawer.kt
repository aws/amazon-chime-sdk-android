/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame

/**
 * [GlVideoFrameDrawer] is a interface for implementing the drawing of video frames and supported buffers to the current EGL surface
 */
interface GlVideoFrameDrawer {
    /**
     * Draw a [VideoFrame] to the current EGL surface using the provided viewport and matrix.
     * [additionalRenderMatrix] will be applied on top of any matrices attached to the video frame buffer.
     * The resulting draw will have the rotation and any internal transform matrices applied.
     *
     * @param frame: [VideoFrame] - Video frame to draw
     * @param viewportX: [Int] - X coordinate of target viewport
     * @param viewportY: [Int] - Y coordinate of target viewport
     * @param viewportWidth: [Int] - Width of target viewport
     * @param viewportHeight: [Int] - Height of target viewport
     * @param additionalRenderMatrix: [Matrix?] - Additional matrix to apply to frame
     */
    fun drawFrame(
        frame: VideoFrame,
        viewportX: Int,
        viewportY: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        additionalRenderMatrix: Matrix?
    )

    /**
     * Deallocate any state or resources held by this object
     */
    fun release()
}
