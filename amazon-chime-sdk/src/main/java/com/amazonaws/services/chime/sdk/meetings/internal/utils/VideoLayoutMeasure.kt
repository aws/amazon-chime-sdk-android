/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.graphics.Point
import android.view.View
import kotlin.math.roundToInt

/**
 * [VideoLayoutMeasure] is a helper class for determining layout size based on
 * layout requirements, scaling type, and video aspect ratio.
 */
class VideoLayoutMeasure {
    enum class ScalingType { SCALE_ASPECT_FIT, SCALE_ASPECT_FILL }

    // Default value, currently not exposed
    val scalingType = ScalingType.SCALE_ASPECT_FILL

    /**
     * Measure desired layout size based off provided parameters
     *
     * @param widthSpec: [Int] - Width value provided by [View.onMeasure]
     * @param heightSpec: [Int] - Height value provided by [View.onMeasure]
     * @param frameWidth: [Int] - Most recent frame width
     * @param frameHeight: [Int] - Most recent frame height
     */
    fun measure(
        widthSpec: Int,
        heightSpec: Int,
        frameWidth: Int,
        frameHeight: Int
    ): Point {
        // Calculate max allowed layout size.
        val maxWidth = View.getDefaultSize(Int.MAX_VALUE, widthSpec)
        val maxHeight =
                View.getDefaultSize(Int.MAX_VALUE, heightSpec)
        if (frameWidth == 0 || frameHeight == 0 || maxWidth == 0 || maxHeight == 0) {
            return Point(maxWidth, maxHeight)
        }
        // Calculate desired display size based on scaling type, video aspect ratio,
        // and maximum layout size.
        val frameAspect = frameWidth / frameHeight.toFloat()
        val layoutSize: Point =
                getDisplaySize(
                        convertScalingTypeToVisibleFraction(scalingType),
                        frameAspect,
                        maxWidth,
                        maxHeight
                )

        // If the measure specification is forcing a specific size, yield.
        // MeasureSpec.EXACTLY implies that parent view should control child size
        if (View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY) {
            layoutSize.x = maxWidth
        }
        if (View.MeasureSpec.getMode(heightSpec) == View.MeasureSpec.EXACTLY) {
            layoutSize.y = maxHeight
        }
        return layoutSize
    }

    /**
     * Each scaling type has a one-to-one correspondence to a numeric minimum fraction of the video
     * that must remain visible.
     */
    private fun convertScalingTypeToVisibleFraction(scalingType: ScalingType): Float {
        return when (scalingType) {
            ScalingType.SCALE_ASPECT_FIT -> 1.0f
            ScalingType.SCALE_ASPECT_FILL -> 0.0f
        }
    }

    /**
     * Calculate display size based on minimum fraction of the video that must remain visible,
     * video aspect ratio, and maximum display size.
     */
    private fun getDisplaySize(
        minVisibleFraction: Float,
        videoAspectRatio: Float,
        maxDisplayWidth: Int,
        maxDisplayHeight: Int
    ): Point {
        // If there is no constraint on the amount of cropping, fill the allowed display area.
        if (minVisibleFraction == 0f || videoAspectRatio == 0f) {
            return Point(maxDisplayWidth, maxDisplayHeight)
        }
        // Each dimension is constrained on max display size and how much we are allowed to crop.
        val width =
                maxDisplayWidth.coerceAtMost(((maxDisplayHeight / minVisibleFraction) * videoAspectRatio).roundToInt())
        val height =
                maxDisplayHeight.coerceAtMost(((maxDisplayWidth / minVisibleFraction) / videoAspectRatio).roundToInt())
        return Point(width, height)
    }
}
