/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.utils

import java.util.GregorianCalendar
import java.util.TimeZone

class TimezoneUtils {
    companion object {
        private const val UTC_OFFSET_FORMAT = "%02d:%02d"

        /**
         * Return utc offset from timezone in +-hh:mm format.
         * E.g Asia/Calcutta timezone is returned as +05:30 UTC offset
         */
        fun getUtcOffset(timezone: TimeZone): String {
            val offsetMillis = timezone.getOffset(GregorianCalendar.getInstance(timezone).timeInMillis)
            val offsetHours = Math.abs((offsetMillis / 60000) / 60)
            val offsetMinutes = Math.abs((offsetMillis / 60000) % 60)
            var offset: String = String.format(UTC_OFFSET_FORMAT, offsetHours, offsetMinutes)
            offset = (if (offsetMillis >= 0) "+" else "-") + offset
            return offset
        }
    }
}
