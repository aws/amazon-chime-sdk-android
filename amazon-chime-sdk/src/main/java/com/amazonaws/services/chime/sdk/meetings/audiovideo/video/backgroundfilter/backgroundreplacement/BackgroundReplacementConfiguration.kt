/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundreplacement

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * A set of options that can be supplied when creating a background replacement video frame processor.
 * @param image: [Bitmap] - An image to replace video background with. Defaults to shaded blue
 * colored image.
 */
class BackgroundReplacementConfiguration @JvmOverloads constructor(var image: Bitmap = createReplacementImage())

private fun createReplacementImage(): Bitmap {
    val width = 250.0f
    val height = 250.0f
    val darkBlueColorString = "#000428"
    val lightBlueColorString = "#004e92"
    val replacementBitmap =
        Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(replacementBitmap)
    val paint = Paint()
    val startColor = Color.parseColor(darkBlueColorString)
    val endColor = Color.parseColor(lightBlueColorString)
    val linearGradient =
        LinearGradient(0.0f, 0.0f, 100.0f, 0.0f, startColor, endColor, Shader.TileMode.CLAMP)
    paint.shader = linearGradient
    canvas.drawRect(0.0f, 0.0f, width, height, paint)
    return replacementBitmap
}
