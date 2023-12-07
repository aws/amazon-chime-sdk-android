/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import kotlin.math.max
import kotlin.math.min

/**
 * [ScreenCaptureResolutionCalculator] calculates scaled resolution based on input resolution
 * and target resolution constraint
 */
class ScreenCaptureResolutionCalculator() {
    // align a positive integer to even number
    fun alignToEven(positiveNumber: Int): Int {
        return positiveNumber and 1.inv()
    }

    // compute target resolution with constraint (targetResolutionShort, targetResolutionLong)
    // high-level description:
    // 1. target resolution constraint is defined by (targetResolutionShort, targetResolutionLong)
    // 2. get min and max display resolution
    // 3. if both short and long display resolutions are within target resolution constraint,
    //    then target resolution is same as display resolution
    // 4. otherwise, we compute target resolution with following steps
    // 4.1. compute resolutionShortScale --> scale factor from displayResolutionShort to targetResolutionShort
    // 4.2. compute resolutionLongScale --> scale factor from displayResolutionLong to targetResolutionLong
    // 4.3. scale the original image using the larger scale (resolutionShortScale or resolutionLongScale)
    // 4.4. scaled image should maintain the same sample aspect ratio and both resolutions should be within target resolution constraint
    fun computeTargetSize(displayWidth: Int, displayHeight: Int, targetResolutionLong: Int, targetResolutionShort: Int): IntArray {
        val displayResolutionShort = min(displayWidth, displayHeight)
        val displayResolutionLong = max(displayWidth, displayHeight)
        val scaledWidth: Int
        val scaledHeight: Int
        val resolutionOverConstraint: Boolean = (displayResolutionShort > targetResolutionShort || displayResolutionLong > targetResolutionLong)
        if (resolutionOverConstraint) {
            val resolutionShortScale: Double = displayResolutionShort.toDouble() / targetResolutionShort.toDouble()
            val resolutionLongScale: Double = displayResolutionLong.toDouble() / targetResolutionLong.toDouble()
            if (resolutionShortScale > resolutionLongScale) {
                if (displayResolutionShort == displayWidth) {
                    scaledWidth = targetResolutionShort
                    scaledHeight = (displayHeight.toDouble() / resolutionShortScale).toInt()
                } else {
                    scaledHeight = targetResolutionShort
                    scaledWidth = (displayWidth.toDouble() / resolutionShortScale).toInt()
                }
            } else {
                if (displayResolutionLong == displayWidth) {
                    scaledWidth = targetResolutionLong
                    scaledHeight = (displayHeight.toDouble() / resolutionLongScale).toInt()
                } else {
                    scaledHeight = targetResolutionLong
                    scaledWidth = (displayWidth.toDouble() / resolutionLongScale).toInt()
                }
            }
        } else {
            scaledWidth = displayWidth
            scaledHeight = displayHeight
        }

        val resolutions = IntArray(2)
        resolutions[0] = scaledWidth
        resolutions[1] = scaledHeight
        return resolutions
    }
}
