/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal

import org.junit.Assert
import org.junit.Test

class AttendeeStatusTest {
    @Test
    fun `from should return object with value from param`() {
        Assert.assertEquals(AttendeeStatus.Joined, AttendeeStatus.from(1))
        Assert.assertEquals(AttendeeStatus.Left, AttendeeStatus.from(2))
        Assert.assertEquals(AttendeeStatus.Dropped, AttendeeStatus.from(3))
    }

    @Test
    fun `from should return null with invalid value from param`() {
        Assert.assertNull(AttendeeStatus.from(4))
    }
}
