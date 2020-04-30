/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import org.junit.Assert.assertEquals
import org.junit.Test

class MeetingSessionConfigurationTest {
    @Test
    fun `constructor should return object with data from parameters`() {
        // Meeting
        val externalMeetingId = "I am the meeting"
        val mediaRegion = "us-east-1"
        val meetingId = "meetingId"

        // Attendee
        val attendeeId = "attendeeId"
        val externalUserId = "Alice"
        val joinToken = "joinToken"

        // MediaPlacement
        val audioFallbackURL = "audioFallbackURL"
        val audioHostURL = "audioHostURL"
        val turnControlURL = "turnControlURL"
        val signalingURL = "signalingURL"

        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                    mediaRegion,
                    meetingId
                )
            ), CreateAttendeeResponse(Attendee(attendeeId, externalUserId, joinToken))
        )

        assertEquals(meetingId, meetingSessionConfiguration.meetingId)
        assertEquals(audioHostURL, meetingSessionConfiguration.urls.audioHostURL)
        assertEquals(audioFallbackURL, meetingSessionConfiguration.urls.audioFallbackURL)
        assertEquals(turnControlURL, meetingSessionConfiguration.urls.turnControlURL)
        assertEquals(signalingURL, meetingSessionConfiguration.urls.signalingURL)
        assertEquals(attendeeId, meetingSessionConfiguration.credentials.attendeeId)
        assertEquals(joinToken, meetingSessionConfiguration.credentials.joinToken)
    }
}
