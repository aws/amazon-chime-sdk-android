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
        val expected = "amazon-chime-sdk-android"
        val actual = DeviceUtils.sdkName
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `osName should return "Android"`() {
        val expected = "Android"
        val actual = DeviceUtils.osName
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `osVersion should return version`() {
        val expected = android.os.Build.VERSION.RELEASE
        val actual = DeviceUtils.osVersion
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `sdkVersion should return value same as BuildConfig`() {
        val expected = BuildConfig.VERSION_NAME
        val actual = DeviceUtils.sdkVersion
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `deviceName should return value same as vendor + model`() {
        val expected = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val actual = DeviceUtils.deviceName
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `deviceModel should return value same as model`() {
        val expected = android.os.Build.MODEL
        val actual = DeviceUtils.deviceModel
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `deviceManufacturer should return value same as vendor`() {
        val expected = android.os.Build.MANUFACTURER
        val actual = DeviceUtils.deviceManufacturer
        Assert.assertEquals(expected, actual)
    }
}
