/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter

/**
 * [ModelShape] Defines the shape of an ML model. This can be used to define the input and
 * output shape of an ML model.
 * @param height: [Int] The number of pixels associated with the height of the input image, defaults to 256.
 * @param width: [Int] The number of pixels associated with the width of the input image, defaults to 144.
 * @param modelRangeMin: [Int] The minimum value associated with the model output, defaults to 0.
 * @param modelRangeMax: [Int] The maximum value associated with the model output, defaults to 1.
 * @param channels: [Int] The number of channels associated with the pixels, defaults to 4 (RGBA).
 */
class ModelShape @JvmOverloads constructor(
    var height: Int = 256,
    var width: Int = 144,
    var modelRangeMin: Int = 0,
    var modelRangeMax: Int = 1,
    var channels: Int = 4
)
