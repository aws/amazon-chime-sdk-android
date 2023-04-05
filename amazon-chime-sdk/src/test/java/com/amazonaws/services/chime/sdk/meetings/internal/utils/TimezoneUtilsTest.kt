/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import java.util.TimeZone
import org.junit.Assert
import org.junit.Test

class TimezoneUtilsTest {
    @Test
    fun `getUtcOffset should return correctly formatted positive Utc offset`() {
        Assert.assertEquals("+05:30", TimezoneUtils.getUtcOffset(TimeZone.getTimeZone("Asia/Calcutta")))
    }

    @Test
    fun `getUtcOffset should return correctly formatted negative Utc offset`() {
        Assert.assertEquals("-06:00", TimezoneUtils.getUtcOffset(TimeZone.getTimeZone("America/Costa_Rica")))
    }
}
