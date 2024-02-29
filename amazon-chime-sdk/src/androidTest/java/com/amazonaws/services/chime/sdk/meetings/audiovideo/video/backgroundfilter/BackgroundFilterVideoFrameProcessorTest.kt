/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur.BackgroundBlurConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur.BackgroundBlurVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement.BackgroundReplacementConfiguration
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement.BackgroundReplacementVideoFrameProcessor
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameRGBABuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdk.test.R
import com.xodee.client.video.JniUtil
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import java.nio.ByteBuffer
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Skipping all tests before setting up real device in code coverage wf")
class BackgroundFilterVideoFrameProcessorTest {

    private lateinit var testBackgroundFilterVideoFrameProcessor: BackgroundFilterVideoFrameProcessor
    private lateinit var testBackgroundBlurVideoFrameProcessor: BackgroundBlurVideoFrameProcessor
    private lateinit var testBackgroundReplacementVideoFrameProcessor: BackgroundReplacementVideoFrameProcessor
    private lateinit var frame: VideoFrame
    private lateinit var rgbaData: ByteBuffer
    private lateinit var filteredByteBuffer: ByteBuffer
    private lateinit var scaledBitmap: Bitmap
    private lateinit var scaledBlurredImageBitmap: Bitmap
    private lateinit var scaledReplacedImageBitmap: Bitmap

    private val logger = ConsoleLogger(LogLevel.INFO)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private val bitmap =
        BitmapFactory.decodeResource(context.resources, R.raw.background_ml_test_image)
    private val blurredImageBitmap =
        BitmapFactory.decodeResource(context.resources, R.raw.background_blurred_image)
    private val replacedImageBitmap =
        BitmapFactory.decodeResource(context.resources, R.raw.background_replaced_image)
    private val staticImageWidth = 720
    private val staticImageHeight = 1280

    private fun checkImageSimilarity(staticImage: Bitmap, testImage: Bitmap): Boolean {
        val differenceThreshold = 10
        var difference: Int
        var differentPixels = 0
        for (row in 0 until testImage.height) {
            for (col in 0 until testImage.width) {
                val staticImageRgb = staticImage.getColor(col, row)
                val testImageRgb = testImage.getColor(col, row)
                // Extracting individual float represented colors and
                // converting them to integer represented colors
                val staticImageRed = (staticImageRgb.red() * 255).toInt()
                val staticImageGreen = (staticImageRgb.green() * 255).toInt()
                val staticImageBlue = (staticImageRgb.blue() * 255).toInt()
                val testImageRed = (testImageRgb.red() * 255).toInt()
                val testImageGreen = (testImageRgb.green() * 255).toInt()
                val testImageBlue = (testImageRgb.blue() * 255).toInt()
                difference = abs(staticImageRed - testImageRed)
                difference += abs(staticImageGreen - testImageGreen)
                difference += abs(staticImageBlue - testImageBlue)
                difference /= 3
                if (difference > differenceThreshold) differentPixels++
            }
        }
        val totalPixels = testImage.width * testImage.height
        val errorRate = differentPixels / totalPixels
        val errorThreshold = 0.01
        return errorRate <= errorThreshold
    }

    @MockK
    private lateinit var mockVideFrameBuffer: VideoFrameRGBABuffer

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        testBackgroundFilterVideoFrameProcessor = BackgroundFilterVideoFrameProcessor(
            logger,
            context,
            "BackgroundFilterVideoFrameProcessor"
        )
        frame = VideoFrame(1L, mockVideFrameBuffer, VideoRotation.Rotation270)
        scaledBitmap = Bitmap.createScaledBitmap(bitmap, 720, 1280, false)
        scaledBlurredImageBitmap = Bitmap.createScaledBitmap(blurredImageBitmap, staticImageWidth, staticImageHeight, false)
        scaledReplacedImageBitmap = Bitmap.createScaledBitmap(replacedImageBitmap, staticImageWidth, staticImageHeight, false)
        rgbaData = ByteBuffer.allocateDirect(scaledBitmap.width * scaledBitmap.height * 4)
        filteredByteBuffer = ByteBuffer.allocateDirect(scaledBitmap.width * scaledBitmap.height * 4)

        scaledBitmap.copyPixelsToBuffer(rgbaData)
        every { mockVideFrameBuffer.height } returns scaledBitmap.height
        every { mockVideFrameBuffer.width } returns scaledBitmap.width
        every { mockVideFrameBuffer.data } returns rgbaData
    }

    @Test
    fun returnInputFrameIfThereIsAnyErrorInProcessingFrame() {
        testBackgroundFilterVideoFrameProcessor.getProcessedFrame(frame, null, rgbaData)
        verify(atLeast = 1) {
            VideoFrameRGBABuffer(
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                rgbaData,
                frame.getRotatedWidth() * 4,
                Runnable { JniUtil.nativeFreeByteBuffer(null) })
        }
    }

    @Test
    fun returnInputFrameWhenFilteredBitmapIsNotNull() {
        mockkStatic(JniUtil::class)
        every { JniUtil.nativeAllocateByteBuffer(any()) } returns filteredByteBuffer

        testBackgroundFilterVideoFrameProcessor.getProcessedFrame(frame, scaledBitmap, rgbaData)

        verify(atLeast = 1) {
            VideoFrameRGBABuffer(
                frame.getRotatedWidth(),
                frame.getRotatedHeight(),
                filteredByteBuffer,
                frame.getRotatedWidth() * 4,
                Runnable { JniUtil.nativeFreeByteBuffer(null) })
        }
    }

    @Test
    fun bitmapHeightShouldBeLessThanWidthInLandscapeOrientation() {
        frame = VideoFrame(1L, mockVideFrameBuffer, VideoRotation.Rotation180)
        val scaledBitmap =
            testBackgroundFilterVideoFrameProcessor.getScaledInputBitmap(frame, bitmap)
        assertTrue(
            "Frame height " + scaledBitmap.height + " should be less than width " + scaledBitmap.width,
            scaledBitmap.height < scaledBitmap.width
        )
    }

    @Test
    fun bitmapHeightShouldBeGreaterThanWidthInPortraitOrientation() {
        val scaledBitmap =
            testBackgroundFilterVideoFrameProcessor.getScaledInputBitmap(frame, bitmap)
        assertTrue(
            "Frame height " + scaledBitmap.height + " should be greater than width " + scaledBitmap.width,
            scaledBitmap.height > scaledBitmap.width
        )
    }

    @Test
    fun getBackgroundBlurredBitmap() {
        // By default input VideoFrame has width > height irrespective of device orientation. Rotated
        // height/weight gives correct dimensions depending on Rotation value. Since input image is
        // portrait (height > width), for testing purpose I flip frames height and width to get
        // correct rotated height based on rotation value.
        every { mockVideFrameBuffer.height } returns scaledBitmap.width
        every { mockVideFrameBuffer.width } returns scaledBitmap.height
        testBackgroundBlurVideoFrameProcessor = BackgroundBlurVideoFrameProcessor(
            logger,
            eglCoreFactory,
            context,
            BackgroundBlurConfiguration()
        )
        val backgroundBlurredBitmap =
            testBackgroundBlurVideoFrameProcessor.getBackgroundBlurredBitmap(scaledBitmap, frame)

        val similarity =
            backgroundBlurredBitmap?.let { checkImageSimilarity(scaledBlurredImageBitmap, it) }
        assertEquals(similarity, true)
    }

    @Test
    fun getBackgroundReplacedBitmap() {
        // By default input VideoFrame has width > height irrespective of device orientation. Rotated
        // height/weight gives correct dimensions depending on Rotation value. Since input image is
        // portrait (height > width), for testing purpose I flip frames height and width to get
        // correct rotated height based on rotation value.
        every { mockVideFrameBuffer.height } returns scaledBitmap.width
        every { mockVideFrameBuffer.width } returns scaledBitmap.height

        testBackgroundReplacementVideoFrameProcessor = BackgroundReplacementVideoFrameProcessor(
            logger,
            eglCoreFactory,
            context,
            BackgroundReplacementConfiguration()
        )
        val backgroundReplacedBitmap =
            testBackgroundReplacementVideoFrameProcessor.getBackgroundReplacedBitmap(
                scaledBitmap,
                frame
            )

        val similarity =
            backgroundReplacedBitmap?.let { checkImageSimilarity(scaledReplacedImageBitmap, it) }
        assertEquals(similarity, true)
    }
}
