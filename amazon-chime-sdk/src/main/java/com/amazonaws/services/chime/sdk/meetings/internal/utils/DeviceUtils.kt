/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.amazonaws.services.chime.sdk.meetings.utils.Versioning
import com.biba.android.MediaClient.BuildConfig

/**
 * [DeviceUtils] stores general device/SDK information
 */
class DeviceUtils {
    companion object {
        val sdkName: String by lazy {
            "amazon-chime-sdk-android"
        }
        val sdkVersion: String by lazy {
            Versioning.sdkVersion()
        }

        val deviceModel: String by lazy {
            android.os.Build.MODEL
        }

        val deviceManufacturer: String by lazy {
            android.os.Build.MANUFACTURER
        }

        val deviceName: String by lazy {
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        }

        val osName: String by lazy {
            "Android"
        }
        val osVersion: String by lazy {
            android.os.Build.VERSION.RELEASE
        }

        val mediaSDKVersion: String by lazy {
            BuildConfig.VERSION_NAME
        }
    }
}
