/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoResolution
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private val ingestionURL = "ingestionURL"

    @Test
    fun `constructor should return object with data from parameters`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
    }
    @Test
    fun `constructor should return object with data from parameters (check default value for meeting features)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters (with Audio feature only)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures()
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (None, None)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("None", "None")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (None, FHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("None", "FHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (None, UHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("None", "UHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionUHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (HD, None)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("HD", "None")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (HD, FHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("HD", "FHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (HD, UHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("HD", "UHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionUHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (FHD, None)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("FHD", "None")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.Disabled, meetingSessionConfiguration.features.contentMaxResolution)
    }

    @Test
    fun `constructor should return object with data from parameters with meeting features (FHD, FHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("FHD", "FHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return object with data from parameters with meeting features (FHD, UHD)`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    externalMeetingId,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL, ingestionURL),
                    mediaRegion,
                    meetingId,
                    MeetingFeatures("FHD", "UHD")
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
        assertEquals(ingestionURL, meetingSessionConfiguration.urls.ingestionURL)
        assertEquals(VideoResolution.VideoResolutionFHD, meetingSessionConfiguration.features.videoMaxResolution)
        assertEquals(VideoResolution.VideoResolutionUHD, meetingSessionConfiguration.features.contentMaxResolution)
    }
    @Test
    fun `constructor should return null externalMeetingId when not provided through Meeting`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    null,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                    mediaRegion,
                    meetingId
                )
            ), CreateAttendeeResponse(Attendee(attendeeId, externalUserId, joinToken))
        )

        assertNull(meetingSessionConfiguration.externalMeetingId)
    }

    @Test
    fun `constructor should return null ingestionUrl when not provided through Meeting`() {
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            CreateMeetingResponse(
                Meeting(
                    null,
                    MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                    mediaRegion,
                    meetingId
                )
            ), CreateAttendeeResponse(Attendee(attendeeId, externalUserId, joinToken))
        )

        assertNull(meetingSessionConfiguration.urls.ingestionURL)
    }

    @Test
    fun `constructor should return null externalMeetingId when not provided through constructor`() {
        val creds = MeetingSessionCredentials(
            attendeeId,
            externalUserId,
            joinToken
        )
        val urls = MeetingSessionURLs(
            audioFallbackURL,
            audioHostURL,
            turnControlURL,
            signalingURL,
            ::defaultUrlRewriter
        )
        val meetingSessionConfiguration = MeetingSessionConfiguration(
            meetingId,
            creds,
            urls
        )

        assertNull(meetingSessionConfiguration.externalMeetingId)
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
