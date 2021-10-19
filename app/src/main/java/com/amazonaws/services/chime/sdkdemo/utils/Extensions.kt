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
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun AppCompatActivity.showToast(msg: String) {
    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
}

fun encodeURLParam(string: String?): String {
    return URLEncoder.encode(string, "utf-8")
}

fun isLandscapeMode(context: Context?): Boolean {
    return context?.let { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE } ?: false
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("hh:mm:ss a").format(Date(timestamp))
}
