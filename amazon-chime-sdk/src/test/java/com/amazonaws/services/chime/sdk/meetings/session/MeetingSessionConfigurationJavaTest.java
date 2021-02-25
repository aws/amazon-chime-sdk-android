package com.amazonaws.services.chime.sdk.meetings.session;

import org.junit.Test;

import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class MeetingSessionConfigurationJavaTest {
    // Meeting
    private String externalMeetingId = "I am the meeting";
    private String mediaRegion = "us-east-1";
    private String meetingId = "meetingId";

    // Attendee
    private String attendeeId = "attendeeId";
    private String externalUserId = "Alice";
    private String joinToken = "joinToken";

    // MediaPlacement
    private String audioFallbackURL = "audioFallbackURL";
    private String audioHostURL = "audioHostURL";
    private String turnControlURL = "turnControlURL";
    private String signalingURL = "signalingURL";
    private CreateMeetingResponse createMeeting = new CreateMeetingResponse(
            new Meeting(
                    externalMeetingId,
                    new MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                    mediaRegion,
                    meetingId
            )
    );
    private CreateAttendeeResponse createAttendee = new CreateAttendeeResponse(new Attendee(attendeeId, externalUserId, joinToken));

    @Test
    public void constructorShouldUseDefaultUrlRewriterWhenNotProvided() {
        MeetingSessionConfiguration meetingSessionConfiguration1 = new MeetingSessionConfiguration(
                createMeeting, createAttendee);

        assertEquals(audioHostURL, meetingSessionConfiguration1.getUrls().getAudioHostURL());
        assertEquals(audioFallbackURL, meetingSessionConfiguration1.getUrls().getAudioFallbackURL());
        assertEquals(signalingURL, meetingSessionConfiguration1.getUrls().getSignalingURL());
        assertEquals(turnControlURL, meetingSessionConfiguration1.getUrls().getTurnControlURL());
    }

    @Test
    public void constructorShouldSetExternalMeetingIdToNullWhenNotProvidedThroughMeeting() {
        MediaPlacement mediaPlacement = new MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL);

        Function1<String, String> urlRewrite = new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                return s;
            }
        };

        MeetingSessionCredentials creds = new MeetingSessionCredentials(
                attendeeId,
                externalUserId,
                joinToken
        );

        MeetingSessionURLs urls = new MeetingSessionURLs(
                mediaPlacement.getAudioFallbackUrl(),
                mediaPlacement.getAudioHostUrl(),
                mediaPlacement.getTurnControlUrl(),
                mediaPlacement.getSignalingUrl(),
                urlRewrite
        );

        MeetingSessionConfiguration meetingSessionConfiguration = new MeetingSessionConfiguration(
                meetingId, creds, urls);

        assertNull(meetingSessionConfiguration.getExternalMeetingId());

    }

    @Test
    public void constructorShouldSetExternalMeetingIdToNullWhenNotProvidedThroughConstructor() {
        CreateMeetingResponse createMeetingNullExternal = new CreateMeetingResponse(
                new Meeting(
                        null,
                        new MediaPlacement(audioFallbackURL, audioHostURL, signalingURL, turnControlURL),
                        mediaRegion,
                        meetingId
                )
        );
        MeetingSessionConfiguration meetingSessionConfiguration = new MeetingSessionConfiguration(
                createMeetingNullExternal, createAttendee);

        assertNull(meetingSessionConfiguration.getExternalMeetingId());
    }

    private String urlRewriter(String url) {
        return url + "a";
    }

    @Test
    public void constructorShouldUseUrlRewriterWhenProvided() {
        Function1<String, String> urlRewrite = new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                return urlRewriter(s);
            }
        };
        MeetingSessionConfiguration meetingSessionConfiguration = new MeetingSessionConfiguration(
                createMeeting,
                createAttendee,
                urlRewrite
        );

        assertEquals(urlRewriter(audioHostURL), meetingSessionConfiguration.getUrls().getAudioHostURL());
        assertEquals(urlRewriter(audioFallbackURL), meetingSessionConfiguration.getUrls().getAudioFallbackURL());
        assertEquals(urlRewriter(signalingURL), meetingSessionConfiguration.getUrls().getSignalingURL());
        assertEquals(urlRewriter(turnControlURL), meetingSessionConfiguration.getUrls().getTurnControlURL());
    }
}
