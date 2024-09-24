/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.content.Context
import com.xodee.client.audio.audioclient.AppInfo
import com.xodee.client.video.VideoClient
import java.util.TimeZone

object AppInfoUtil {
    private lateinit var manufacturer: String
    private lateinit var model: String
    private lateinit var osVersion: String
    private lateinit var appName: String
    private lateinit var appCode: String
    private const val clientSource = "amazon-chime-sdk"
    private lateinit var sdkVersion: String
    private lateinit var clientUtcOffset: String

    private fun initializeAppInfo(context: Context) {
        manufacturer = DeviceUtils.deviceManufacturer.toString()
        model = DeviceUtils.deviceModel.toString()
        osVersion = DeviceUtils.osVersion.toString()
        val packageName = context.packageName
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        appName = String.format("Android %s", packageInfo.versionName)
        appCode = packageInfo.versionCode.toString()
        sdkVersion = DeviceUtils.sdkVersion
        val deviceTimezone = TimeZone.getDefault()
        clientUtcOffset = TimezoneUtils.getUtcOffset(deviceTimezone)
    }

    fun initializeVideoClientAppDetailedInfo(context: Context) {
        initializeAppInfo(context)

        VideoClient.AppDetailedInfo.initialize(
            appName,
            appCode,
            model,
            manufacturer,
            osVersion,
            clientSource,
            sdkVersion,
            clientUtcOffset
        )
    }

    fun initializeAudioClientAppInfo(context: Context): AppInfo {
        initializeAppInfo(context)

        return AppInfo(
            appName,
            appCode,
            manufacturer,
            model,
            osVersion,
            clientSource,
            sdkVersion,
            clientUtcOffset
        )
    }
}
