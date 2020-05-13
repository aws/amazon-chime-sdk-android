/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Test

class MeetingSessionConfigurationTest {
    // Meeting
    private val externalMeetingId = "I am the meeting"
    private val mediaRegion = "us-east-1"
    private val meetingId = "meetingId"

    // Attendee
    private val attendeeId = "attendeeId"
    private val externalUserId = "Alice"
    private val joinToken = "joinToken"

    // MediaPlacement
    private val audioFallbackURL = "audioFallbackURL"
    private val audioHostURL = "audioHostURL"
    private val turnControlURL = "turnControlURL"
    private val signalingURL = "signalingURL"

    @Test
    fun `constructor should return object with data from parameters`() {
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

    @Test
    fun `UrlRewriter should replace given urls`() {
        val mockRewrite = spyk(::mockUrlRewriter)
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                    mediaRegion,
                    meetingId
                )
            ), CreateAttendeeResponse(Attendee(attendeeId, externalUserId, joinToken)),
            mockRewrite)

        assertEquals(replaceString(audioHostURL),
            meetingSessionConfiguration.urls.audioHostURL)
        assertEquals(replaceString(audioFallbackURL),
            meetingSessionConfiguration.urls.audioFallbackURL)
        assertEquals(replaceString(turnControlURL),
            meetingSessionConfiguration.urls.turnControlURL)
        assertEquals(replaceString(signalingURL),
            meetingSessionConfiguration.urls.signalingURL)
    }

    private fun replaceString(str: String): String {
        return str.replace("URL", "Hello")
    }

    private fun mockUrlRewriter(url: String): String {
        return replaceString(url)
    }
}
