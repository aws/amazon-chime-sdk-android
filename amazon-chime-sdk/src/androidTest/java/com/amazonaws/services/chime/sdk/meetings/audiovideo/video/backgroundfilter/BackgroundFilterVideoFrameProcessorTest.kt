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
import io.mockk.verify
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Works on real device, seeing issue when run on emulator")
class BackgroundFilterVideoFrameProcessorTest {

    private lateinit var testBackgroundFilterVideoFrameProcessor: BackgroundFilterVideoFrameProcessor
    private lateinit var testBackgroundBlurVideoFrameProcessor: BackgroundBlurVideoFrameProcessor
    private lateinit var testBackgroundReplacementVideoFrameProcessor: BackgroundReplacementVideoFrameProcessor
    private lateinit var frame: VideoFrame
    private lateinit var rgbaData: ByteBuffer
    private lateinit var scaledBitmap: Bitmap

    private val logger = ConsoleLogger(LogLevel.INFO)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private val bitmap =
        BitmapFactory.decodeResource(context.resources, R.raw.background_ml_test_image)
    private val BACKGROUND_BLURRED_IMAGE_HASH =
        "a7a466655377225e391828357b5b4aa69b15ce0a56790f778180e806e3c0ad21"
    private val BACKGROUND_REPLACED_IMAGE_HASH =
        "767553ffe7945101e9beae609f33970e68648037bba9fe2e149e6ec4a06528c6"

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
        rgbaData = ByteBuffer.allocateDirect(scaledBitmap.width * scaledBitmap.height * 4)
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
        testBackgroundBlurVideoFrameProcessor = BackgroundBlurVideoFrameProcessor(
            logger,
            eglCoreFactory,
            context,
            BackgroundBlurConfiguration()
        )
        val backgroundBlurredBitmap =
            testBackgroundBlurVideoFrameProcessor.getBackgroundBlurredBitmap(scaledBitmap, frame)

        val byteArrayOutputStream = ByteArrayOutputStream()
        backgroundBlurredBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bitmapBytes = byteArrayOutputStream.toByteArray()
        val hashString = getHexString(bitmapBytes)
        assertEquals(BACKGROUND_BLURRED_IMAGE_HASH, hashString)
    }

    @Test
    fun getBackgroundReplacedBitmap() {
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

        val byteArrayOutputStream = ByteArrayOutputStream()
        backgroundReplacedBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bitmapBytes = byteArrayOutputStream.toByteArray()
        val hashString = getHexString(bitmapBytes)

        assertEquals(BACKGROUND_REPLACED_IMAGE_HASH, hashString)
    }

    private fun getHexString(byteArray: ByteArray?): String {
        // Generate checksum of the test file content.
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(byteArray)
        val hash = digester.digest()

        // Create Hex String.
        val hexString: StringBuilder = StringBuilder()
        for (byte: Byte in hash) {
            var str: String = Integer.toHexString(0xFF and byte.toInt())
            while (str.length < 2)
                str = "0$str"
            hexString.append(str)
        }
        return hexString.toString()
    }
}
