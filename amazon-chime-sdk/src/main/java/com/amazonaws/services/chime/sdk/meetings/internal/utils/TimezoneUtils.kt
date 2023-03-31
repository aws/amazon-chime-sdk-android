/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import java.util.GregorianCalendar
import java.util.TimeZone

class TimezoneUtils {
    companion object {

        /**
         * Return utc offset from timezone in +- hh:mm format.
         * E.g Asia/Calcutta timezone is returned as +05:30 UTC offset
         */
        fun getUtcOffset(timezone: TimeZone): String {
            val offsetInMillis = timezone.getOffset(GregorianCalendar.getInstance(timezone).timeInMillis)
            var offset: String = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60))
            offset = (if (offsetInMillis >= 0) "+" else "-") + offset
            return offset
        }
    }
}
