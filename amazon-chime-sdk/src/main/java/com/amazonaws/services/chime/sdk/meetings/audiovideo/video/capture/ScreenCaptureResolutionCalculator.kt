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
    // 5. After calculation of scaledWidth and scaledHeight, 2-byte alignment is done (to handle 420 color space conversion)
    fun computeTargetSize(displayWidth: Int, displayHeight: Int): Int {
        val displayResolutionMin = min(displayWidth, displayHeight)
        val displayResolutionMax = max(displayWidth, displayHeight)
        val scaledWidth: Int
        val scaledHeight: Int
        if (displayResolutionMin > targetMinVal || displayResolutionMax > targetMaxVal) {
            val resolutionMinScale: Double = displayResolutionMin.toDouble() / targetMinVal.toDouble()
            val resolutionMaxScale: Double = displayResolutionMax.toDouble() / targetMaxVal.toDouble()
            if (resolutionMinScale > resolutionMaxScale) {
                if (displayResolutionMin == displayWidth) {
                    scaledWidth = targetMinVal
                    scaledHeight = (displayHeight.toDouble() / resolutionMinScale.toDouble()).toInt()
                } else {
                    scaledHeight = targetMinVal
                    scaledWidth = (displayWidth.toDouble() / resolutionMinScale.toDouble()).toInt()
                }
            } else {
                if (displayResolutionMax == displayWidth) {
                    scaledWidth = targetMaxVal
                    scaledHeight = (displayHeight.toDouble() / resolutionMaxScale.toDouble()).toInt()
                } else {
                    scaledHeight = targetMaxVal
                    scaledWidth = (displayWidth.toDouble() / resolutionMaxScale.toDouble()).toInt()
                }
            }
        } else {
            scaledWidth = displayWidth
            scaledHeight = displayHeight
        }

        val mask: Int = 1
        // align width and height to 2-byte
        val alignedWidth: Int = scaledWidth and mask.inv()
        val alignedHeight: Int = scaledHeight and mask.inv()
        val resolution: Int = ((alignedHeight shl 16) or (alignedWidth and 0xffff))
        return resolution
    }
}
