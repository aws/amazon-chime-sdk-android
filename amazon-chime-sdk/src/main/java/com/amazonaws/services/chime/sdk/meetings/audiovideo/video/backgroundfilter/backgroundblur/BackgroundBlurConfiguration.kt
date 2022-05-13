/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur

/**
 * A set of options that can be supplied when creating a background blur video frame processor.
 * @param blurStrength: [Float] - Supported range 0 < blurStrength <= 25.
 * See https://developer.android.com/reference/android/renderscript/ScriptIntrinsicBlur#setRadius(float).
 * Defaults to value of 7.0f.
 */
class BackgroundBlurConfiguration @JvmOverloads constructor(var blurStrength: Float = 7.0f)
