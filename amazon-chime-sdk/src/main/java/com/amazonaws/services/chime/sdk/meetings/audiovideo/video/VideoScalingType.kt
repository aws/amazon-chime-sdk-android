/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoScalingType] describes the scaling type of how video is rendered.  Certain types
 * may effect how much of a video is cropped. [visibleFraction] refers to the minimum amount
 * of a video frame required to be shown per scaling type (e.g. [AspectFit] indicates showing
 * the whole frame, no cropping).
 */
enum class VideoScalingType(val visibleFraction: Float) {
    /**
     * Fit the frame to the surrounding view to avoid any cropping.
     */
    AspectFit(1.0f),

    /**
     * Attempt to avoid cropping seen using [AspectFill] while showing more
     * of the image then [AspectFit]; this may crop if the aspect ratios do not match.
     */
    AspectBalanced(0.5625f),

    /**
     * Fill the surrounding view; this may crop if the aspect ratios do not match.
     */
    AspectFill(0.0f)
}
