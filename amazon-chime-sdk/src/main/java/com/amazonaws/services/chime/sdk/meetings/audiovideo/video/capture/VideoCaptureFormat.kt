/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

/**
 * [VideoCaptureFormat] describes a given capture format that can be set to a [VideoCaptureSource].
 * Note that [VideoCaptureSource] implementations may ignore or adjust unsupported values.
 */
data class VideoCaptureFormat(
    /**
     * Capture width
     */
    val width: Int,

    /**
     * Capture height
     */
    val height: Int,

    /**
     * Max FPS. When used as input this implies the desired FPS as well
     */
    val maxFps: Int
) {
    init {
        check(width >= 0 && height >= 0) { "Width and height must be positive" }
    }

    override fun toString(): String {
        return "$width x $height @ $maxFps FPS"
    }
}
