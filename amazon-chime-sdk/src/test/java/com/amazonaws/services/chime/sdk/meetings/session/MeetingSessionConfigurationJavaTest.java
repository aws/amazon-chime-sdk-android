package com.amazonaws.services.chime.sdk.meetings.session;

import org.junit.Test;

import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
