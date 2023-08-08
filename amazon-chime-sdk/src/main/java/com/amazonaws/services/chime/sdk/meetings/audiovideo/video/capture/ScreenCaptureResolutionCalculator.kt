/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import kotlin.math.max
import kotlin.math.min
class ScreenCaptureResolutionCalculator(
    private val targetMinVal: Int,
    private val targetMaxVal: Int
) {
    // align a positive integer to even number
    fun alignToEven(positiveNumber: Int): Int {
        return positiveNumber and 1.inv()
    }

    // compute target resolution with constraint (targetMinVal, targetMaxVal)
    // high-level description:
    // 1. target resolution constraint is defined by (targetMinVal, targetMaxVal)
    // 2. get min and max display resolution
    // 3. if both min and max display resolutions are within target resolution constraint,
    //    then target resolution is same as display resolution
    // 4. otherwise, we compute target resolution with following steps
    // 4.1. compute resolutionMinScale --> scale factor from displayResolutionMin to targetResolutionMin
    // 4.2. compute resolutionMaxScale --> scale factor from displayResolutionMax to targetResolutionMax
    // 4.3. scale the original image using the larger scale (resolutionMinScale or resolutionMaxScale)
    // 4.4. scaled image should maintain the same sample aspect ratio and both resolutions should be within target resolution constraint
    fun computeTargetSize(displayWidth: Int, displayHeight: Int): IntArray {
        val displayResolutionMin = min(displayWidth, displayHeight)
        val displayResolutionMax = max(displayWidth, displayHeight)
        val scaledWidth: Int
        val scaledHeight: Int
        val resolutionOverConstraint: Boolean = (displayResolutionMin > targetMinVal || displayResolutionMax > targetMaxVal)
        if (resolutionOverConstraint) {
            val resolutionMinScale: Double = displayResolutionMin.toDouble() / targetMinVal.toDouble()
            val resolutionMaxScale: Double = displayResolutionMax.toDouble() / targetMaxVal.toDouble()
            if (resolutionMinScale > resolutionMaxScale) {
                if (displayResolutionMin == displayWidth) {
                    scaledWidth = targetMinVal
                    scaledHeight = (displayHeight.toDouble() / resolutionMinScale).toInt()
                } else {
                    scaledHeight = targetMinVal
                    scaledWidth = (displayWidth.toDouble() / resolutionMinScale).toInt()
                }
            } else {
                if (displayResolutionMax == displayWidth) {
                    scaledWidth = targetMaxVal
                    scaledHeight = (displayHeight.toDouble() / resolutionMaxScale).toInt()
                } else {
                    scaledHeight = targetMaxVal
                    scaledWidth = (displayWidth.toDouble() / resolutionMaxScale).toInt()
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
