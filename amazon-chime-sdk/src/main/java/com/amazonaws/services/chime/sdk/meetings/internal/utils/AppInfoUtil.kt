/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.content.Context
import com.xodee.client.video.VideoClient

object AppInfoUtil {
    fun initializeVideoClientAppDetailedInfo(context: Context) {
        val manufacturer = DeviceUtils.deviceManufacturer
        val model = DeviceUtils.deviceModel
        val osVersion = DeviceUtils.osVersion
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val appVer = packageInfo.versionName
        val appCode = packageInfo.versionCode.toString()
        val clientSource = "amazon-chime-sdk"
        val sdkVersion = DeviceUtils.sdkVersion

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
