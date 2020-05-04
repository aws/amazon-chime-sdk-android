/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

import java.io.FileInputStream
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Test

class VersioningTest {
    @Test
    fun `sdkVersion should return version string matching with properties file`() {
        val sdkVersion = Versioning.sdkVersion()
        val file = FileInputStream("version.properties")
        val versionProp = Properties()

        versionProp.load(file)
        val version = "${versionProp.getProperty("versionMajor")}.${versionProp.getProperty("versionMinor")}.${versionProp.getProperty("versionPatch")}"

        assertEquals(sdkVersion, version)
    }
}
