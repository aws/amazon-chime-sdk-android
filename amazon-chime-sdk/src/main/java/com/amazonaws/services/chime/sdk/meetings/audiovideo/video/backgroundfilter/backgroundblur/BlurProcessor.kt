/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.backgroundfilter.backgroundblur

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.Type
import java.lang.Exception

/**
 * [BlurProcessor] Creates input and output allocations and runs gaussian blur on each element of
 * the bitmap.
 * @param rs: [RenderScript] - RenderScript instance used to create Gaussian image blur script.
 */
internal class BlurProcessor(rs: RenderScript) {
    private val rs: RenderScript = rs
    private var inAllocation: Allocation? = null
    private var outAllocation: Allocation? = null
    private var width = 0
    private var height = 0

    // Gaussian image blur script.
    private var blurScript: ScriptIntrinsicBlur? = null

    fun initialize(width: Int, height: Int, blurStrength: Float) {
        this.width = width
        this.height = height
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blurScript?.setRadius(blurStrength) // Range 0 < radius <= 25.
        if (outAllocation != null) {
            outAllocation?.destroy()
            outAllocation = null
        }

        // Bitmap must have ARGB_8888 config for this type.
        val bitmapType = Type.Builder(rs, Element.RGBA_8888(rs))
            .setX(width)
            .setY(height)
            .setMipmaps(false)
            .create()

        // Create output allocation.
        outAllocation = Allocation.createTyped(rs, bitmapType)

        // Create input allocation with same type as output allocation.
        inAllocation = Allocation.createTyped(rs, bitmapType)
    }

    fun release() {
        blurScript?.destroy()
        blurScript = null

        inAllocation?.destroy()
        inAllocation = null

        outAllocation?.destroy()
        outAllocation = null
    }

    /**
     * Apply blur effect on each element inside an allocation.
     */
    fun process(bitmap: Bitmap): Bitmap? {
        val bitmapConfig = Bitmap.Config.ARGB_8888
        if (bitmap.config?.equals(bitmapConfig) == false) {
            throw Exception("Bitmap must have ARGB_8888 config.")
        }
        // Copy data from bitmap to input allocations.
        inAllocation?.copyFrom(bitmap)

        // Set input for blur script.
        blurScript?.setInput(inAllocation)

        // Process and set data to the output allocation.
        blurScript?.forEach(outAllocation)
        val blurredBitmap = Bitmap.createBitmap(this.width, this.height, bitmapConfig)
        outAllocation?.copyTo(blurredBitmap)
        return blurredBitmap
    }
}
