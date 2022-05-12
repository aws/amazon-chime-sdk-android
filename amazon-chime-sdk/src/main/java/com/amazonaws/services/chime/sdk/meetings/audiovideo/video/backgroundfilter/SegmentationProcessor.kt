/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.amazonaws.services.chime.cwt.InputModelConfig
import com.amazonaws.services.chime.cwt.ModelState
import com.amazonaws.services.chime.cwt.PredictResult
import com.amazonaws.services.chime.cwt.TfLiteModel
import java.nio.ByteBuffer

/**
 * [SegmentationProcessor] predicts foreground mask for an image.
 */
class SegmentationProcessor(val context: Context) {
    private lateinit var segmentationModel: TfLiteModel
    lateinit var modelState: ModelState
    private val bytes: ByteArray =
        context.assets.open("selfie_segmentation_landscape.tflite").readBytes()

    /**
     * Initialize and load the tensorflow model.
     */
    fun initialize(width: Int, height: Int, modelShape: ModelShape) {
        if (isMLlibraryLoaded) {
            segmentationModel = TfLiteModel()
        }
        val config = InputModelConfig(height, width, modelShape.channels, modelShape.modelRangeMin, modelShape.modelRangeMax)
        modelState = segmentationModel.loadModelBytes(bytes, config)
    }

    /**
     * Predicts the foreground on the input image.
     *
     * @return [PredictResult] value of type int.
     */
    fun predict(): PredictResult {
        return segmentationModel.predict()
    }

    /**
     * Retrieve input image on which segmentation can be applied.
     */
    fun getInputBuffer(): ByteBuffer {
        return segmentationModel.getInputBuffer()
    }

    /**
     * Retrieve segmented image mask.
     */
    fun getOutputBuffer(): ByteBuffer {
        return segmentationModel.getOutputBuffer()
    }

    @SuppressLint("LongLogTag")
    companion object {
        var isMLlibraryLoaded = false
        init {
            try {
                System.loadLibrary("amazon-chime-sdk-machine-learning_jni")
                isMLlibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(
                    "BackgroundFilterVideoFrameProcessor",
                    "Unable to load Machine Learning Library. " +
                            "Use `implementation(name: 'amazon-chime-sdk-machine-learning', ext: 'aar')`" +
                            " under dependencies in build.gradle file."
                )
            }
        }
    }
}
