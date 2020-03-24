/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingSessionStatusTest {
    private val testStatusCode = MeetingSessionStatusCode.OK

    @Test
    fun `constructor should return object with value from param`() {
        val testOutput = MeetingSessionStatus(testStatusCode)

        assertNotNull(testOutput)
        assertEquals(testStatusCode, testOutput.statusCode)
    }

    @Test
    fun `secondary constructor should return object with non null status code when using defined values`() {
        val testOutput = MeetingSessionStatus(0)

        assertNotNull(testOutput)
        assertEquals(testStatusCode, testOutput.statusCode)
    }

    @Test
    fun `secondary constructor should return status with null status code when using undefined values`() {
        val testOutput = MeetingSessionStatus(-99)

        assertNotNull(testOutput)
        assertNull("actual: ${testOutput.statusCode}", testOutput.statusCode)
    }
}
