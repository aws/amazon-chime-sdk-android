/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.ScreenCaptureResolutionCalculator
import kotlin.math.max
import kotlin.math.min
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenCaptureResolutionCalculatorTest {

    private val targetMinVal: Int = 1080
    private val targetMaxVal: Int = 1920
    private val screenCaptureResolutionCalculator: ScreenCaptureResolutionCalculator = ScreenCaptureResolutionCalculator(this.targetMinVal, this.targetMaxVal)

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `calculate resolution below target`() {
        // compute targetWidth and targetHeight with alignment
        val width: Int = 1280
        val height: Int = 720
        val targetSize: Int = screenCaptureResolutionCalculator!!.computeTargetSize(width, height)
        val alignedWidth: Int = targetSize and 0xffff
        val alignedHeight: Int = targetSize shr 16

        assertTrue(alignedWidth <= width && alignedHeight <= height)
    }

    @Test
    fun `calculate resolution above target (both dimensions)`() {
        // compute targetWidth and targetHeight with alignment
        val width: Int = 3840
        val height: Int = 2160
        val targetSize: Int = screenCaptureResolutionCalculator!!.computeTargetSize(width, height)
        val alignedWidth: Int = targetSize and 0xffff
        val alignedHeight: Int = targetSize shr 16
        val maxVal: Int = max(alignedWidth, alignedHeight)
        val minVal: Int = min(alignedWidth, alignedHeight)
        assert(minVal <= targetMinVal && maxVal <= targetMaxVal)
    }

    @Test
    fun `calculate resolution above target (maxVal over limit)`() {
        // compute targetWidth and targetHeight with alignment
        val width: Int = 3840
        val height: Int = 720
        val targetSize: Int = screenCaptureResolutionCalculator!!.computeTargetSize(width, height)
        val alignedWidth: Int = targetSize and 0xffff
        val alignedHeight: Int = targetSize shr 16
        val maxVal: Int = max(alignedWidth, alignedHeight)
        val minVal: Int = min(alignedWidth, alignedHeight)
        assert(minVal <= targetMinVal && maxVal <= targetMaxVal)
    }

    @Test
    fun `calculate resolution above target (minVal over limit)`() {
        // compute targetWidth and targetHeight with alignment
        val width: Int = 1920
        val height: Int = 1280
        val targetSize: Int = screenCaptureResolutionCalculator!!.computeTargetSize(width, height)
        val alignedWidth: Int = targetSize and 0xffff
        val alignedHeight: Int = targetSize shr 16
        val maxVal: Int = max(alignedWidth, alignedHeight)
        val minVal: Int = min(alignedWidth, alignedHeight)
        assert(minVal <= targetMinVal && maxVal <= targetMaxVal)
    }
}
