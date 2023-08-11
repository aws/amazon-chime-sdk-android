/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCaptureResolutionCalculatorTest {

    // this threshold is from our tests (max aspect ratio difference in the tests is 8 and we set the threshold to 12 here)
    private val aspectRatioDiffThreshold: Int = 12
    private val targetMinVal: Int = 1080
    private val targetMaxVal: Int = 1920
    private val screenCaptureResolutionCalculator: ScreenCaptureResolutionCalculator = ScreenCaptureResolutionCalculator(this.targetMinVal, this.targetMaxVal)

    @Test
    fun `alignToEven should return original even number when input is even number`() {
        val origNumber: Int = 1280
        val alignedNumber:Int = screenCaptureResolutionCalculator.alignToEven(origNumber)
        assertTrue(origNumber == alignedNumber)
    }

    @Test
    fun `alignToEven should return a smaller even number when input is odd number`() {
        val origNumber: Int = 1281
        val alignedNumber:Int = screenCaptureResolutionCalculator.alignToEven(origNumber)
        assertTrue(origNumber == alignedNumber+1)
    }

    fun nonScaleTest(width: Int, height: Int) {
        val targetSize = screenCaptureResolutionCalculator.computeTargetSize(width, height)
        val scaledWidth: Int = targetSize[0]
        val scaledHeight: Int = targetSize[1]
        assertTrue(width == scaledWidth && height == scaledHeight)
    }

    fun scaleTest(width: Int, height: Int) {
        val targetSize = screenCaptureResolutionCalculator.computeTargetSize(width, height)
        val scaledWidth: Int = targetSize[0]
        val scaledHeight: Int = targetSize[1]
        val maxVal: Int = max(scaledWidth, scaledHeight)
        val minVal: Int = min(scaledWidth, scaledHeight)
        val originalAspectRatio: Int = (1000 * width.toDouble() / height.toDouble()).toInt()
        val scaledAspectRatio: Int = (1000 * scaledWidth.toDouble() / scaledHeight.toDouble()).toInt()
        val aspectRatioDiff: Int = abs(scaledAspectRatio - originalAspectRatio)
        assert(minVal <= targetMinVal && maxVal <= targetMaxVal && aspectRatioDiff < aspectRatioDiffThreshold)
    }

    @Test
    fun `computeTargetSize(landscape) should return original resolution when max(width, height) is below targetMaxVal and min(width, height) is below targetMinVal`() {
        nonScaleTest(1280, 720)
    }

    @Test
    fun `computeTargetSize(landscape) should return original resolution when max(width, height) is equal targetMaxVal and min(width, height) is below targetMinVal`() {
        nonScaleTest(1920, 719)
    }

    @Test
    fun `computeTargetSize(landscape) should return original resolution when max(width, height) is below targetMaxVal and min(width, height) is equal targetMinVal`() {
        nonScaleTest(1280, 1080)
    }

    @Test
    fun `computeTargetSize(landscape) should return original resolution when max(width, height) is equal targetMaxVal and min(width, height) is equal targetMinVal`() {
        nonScaleTest(1920, 1080)
    }

    @Test
    fun `computeTargetSize(weird number) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(7139, 3217)
    }

    @Test
    fun `computeTargetSize(extra large number) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(93215327, 32171121)
    }

    @Test
    fun `computeTargetSize(landscape) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(3840, 1423)
    }

    @Test
    fun `computeTargetSize(landscape) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is below targetMinVal`() {
        scaleTest(3840, 720)
    }

    @Test
    fun `computeTargetSize(landscape) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is equal targetMinVal`() {
        scaleTest(3840, 1080)
    }

    @Test
    fun `computeTargetSize(landscape) should return scaled resolution when max(width, height) is equal targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(1920, 1280)
    }

    @Test
    fun `computeTargetSize(landscape) should return scaled resolution when max(width, height) is below targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(1600, 1280)
    }

    @Test
    fun `computeTargetSize(portrait) should return original resolution when max(width, height) is below targetMaxVal and min(width, height) is below targetMinVal`() {
        nonScaleTest(720, 1280)
    }

    @Test
    fun `computeTargetSize(portrait) should return original resolution when max(width, height) is equal targetMaxVal and min(width, height) is below targetMinVal`() {
        nonScaleTest(719, 1920)
    }

    @Test
    fun `computeTargetSize(portrait) should return original resolution when max(width, height) is below targetMaxVal and min(width, height) is equal targetMinVal`() {
        nonScaleTest(1080, 1280)
    }

    @Test
    fun `computeTargetSize(portrait) should return original resolution when max(width, height) is equal targetMaxVal and min(width, height) is equal targetMinVal`() {
        nonScaleTest(1080, 1920)
    }

    @Test
    fun `computeTargetSize(portrait) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(1423, 3840)
    }

    @Test
    fun `computeTargetSize(portrait) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is below targetMinVal`() {
        scaleTest(720, 3840)
    }

    @Test
    fun `computeTargetSize(portrait) should return scaled resolution when max(width, height) is above targetMaxVal and min(width, height) is equal targetMinVal`() {
        scaleTest(1080, 3840)
    }

    @Test
    fun `computeTargetSize(portrait) should return scaled resolution when max(width, height) is equal targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(1280, 1920)
    }

    @Test
    fun `computeTargetSize(portrait) should return scaled resolution when max(width, height) is below targetMaxVal and min(width, height) is above targetMinVal`() {
        scaleTest(1280, 1600)
    }
}