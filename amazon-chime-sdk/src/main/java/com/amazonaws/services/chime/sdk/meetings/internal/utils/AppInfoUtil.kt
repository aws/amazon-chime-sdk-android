/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.content.Context
import com.amazonaws.services.chime.sdk.BuildConfig
import com.xodee.client.video.VideoClient

object AppInfoUtil {
    fun initializeVideoClientAppDetailedInfo(context: Context) {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val appVer = packageInfo.versionName
        val appCode = packageInfo.versionCode.toString()
        val clientSource = "amazon-chime-sdk"
        val sdkVersion = BuildConfig.VERSION_NAME

        VideoClient.AppDetailedInfo.initialize(
            String.format("Android %s", appVer),
            appCode,
            model,
            manufacturer,
            osVersion,
            clientSource,
            sdkVersion
        )
    }
}
