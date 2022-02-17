package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/*
 * A video source available in the current meeting. These need to be consistent between `remoteVideoSourcesDidBecomeAvailable`
 * and `updateVideoSourceSubscriptions` as they are used as keys in maps that may be updated. I.e. when setting up
 * a map for `updateVideoSourceSubscriptions` do not construct these yourselves or the configuration may or may not
 * be updated.
 *
 * @property attendeeId: [String] - Attendee ID for the given source
 */
class RemoteVideoSource(val attendeeId: String)
