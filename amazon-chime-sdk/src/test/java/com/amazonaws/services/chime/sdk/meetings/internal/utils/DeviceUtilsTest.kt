/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import com.amazonaws.services.chime.sdk.BuildConfig
import org.junit.Assert
import org.junit.Test

class DeviceUtilsTest {
    @Test
    fun `sdkName should return "amazon-chime-sdk-android"`() {
        Assert.assertEquals("amazon-chime-sdk-android", DeviceUtils.sdkName)
    }

    @Test
    fun `osName should return "Android"`() {
        Assert.assertEquals("Android", DeviceUtils.osName)
    }

    @Test
    fun `osVersion should return "Android"`() {
        Assert.assertEquals(android.os.Build.VERSION.RELEASE, DeviceUtils.osVersion)
    }

    @Test
    fun `sdkVersion should return value same as BuildConfig`() {
        Assert.assertEquals(BuildConfig.VERSION_NAME, DeviceUtils.sdkVersion)
    }

    @Test
    fun `deviceName should return value same as vendor + model`() {
        Assert.assertEquals("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", DeviceUtils.deviceName)
    }

    @Test
    fun `deviceModel should return value same as model`() {
        Assert.assertEquals(android.os.Build.MODEL, DeviceUtils.deviceModel)
    }

    @Test
    fun `deviceManufacturer should return value same as vendor`() {
        Assert.assertEquals(android.os.Build.MANUFACTURER, DeviceUtils.deviceManufacturer)
    }
}
