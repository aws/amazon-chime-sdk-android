/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingSessionStatusCodeTest {
    @Test
    fun `from should return MeetingSessionStatusCode when defined value`() {
        assertEquals(
            MeetingSessionStatusCode.OK,
            MeetingSessionStatusCode.from(0)
        )
        assertEquals(
            MeetingSessionStatusCode.AudioJoinedFromAnotherDevice,
            MeetingSessionStatusCode.from(2)
        )
        assertEquals(
            MeetingSessionStatusCode.AudioCallEnded,
            MeetingSessionStatusCode.from(6)
        )
        assertEquals(
            MeetingSessionStatusCode.AudioDisconnected,
            MeetingSessionStatusCode.from(9)
        )
    }

    @Test
    fun `from should return null when undefined value`() {
        assertNull(MeetingSessionStatusCode.from(-99))
    }
}
