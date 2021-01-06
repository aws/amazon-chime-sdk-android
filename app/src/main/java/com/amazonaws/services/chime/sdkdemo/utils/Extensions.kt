/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import java.net.URLEncoder

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun encodeURLParam(string: String?): String {
    return URLEncoder.encode(string, "utf-8")
}

fun isLandscapeMode(context: Context?): Boolean {
    return context?.let { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE } ?: false
}
