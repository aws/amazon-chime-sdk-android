/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.RemoteVideoSource
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
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

fun String.isContentShare(): Boolean {
    return DefaultModality(this).hasModality(ModalityType.Content)
}

fun RemoteVideoSource.isContentShare(): Boolean {
    return this.attendeeId.isContentShare()
}

fun isOSVersionAtLeast(targetVersion: Int): Boolean = Build.VERSION.SDK_INT >= targetVersion

// Starting Android 15, app is rendering in edge-to-edge mode by default
// This is to remove overlap between system bars and content
// Also if keyboard is displayed, align the bottom with its top
fun addPaddingsForSystemBars(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(bottom = insets.bottom, top = insets.top, left = insets.left, right = insets.right)
        if (windowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(bottom = imeInsets.bottom)
        }
        WindowInsetsCompat.CONSUMED
    }
}
