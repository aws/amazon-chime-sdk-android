package com.amazon.chime.sdk.session

import org.junit.Assert.assertEquals
import org.junit.Test

class MeetingSessionConfigurationTest {
    @Test
    fun `constructor should return object with data from parameters`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(Meeting("meetingId", MediaPlacement("audioHostUrl"))),
            CreateAttendeeResponse(Attendee("attendeeId", "joinToken"))
        )
        assertEquals("meetingId", meetingSessionConfiguration.meetingId)
        assertEquals("audioHostUrl", meetingSessionConfiguration.urls.audioHostURL)
        assertEquals("attendeeId", meetingSessionConfiguration.credentials.attendeeId)
        assertEquals("joinToken", meetingSessionConfiguration.credentials.joinToken)
    }
}
