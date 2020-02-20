/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

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
