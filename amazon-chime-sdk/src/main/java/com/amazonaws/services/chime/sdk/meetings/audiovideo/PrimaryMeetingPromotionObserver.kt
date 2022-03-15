package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionStatus

/**
 * [PrimaryMeetingPromotionObserver] handles events related to the promotion and demotion
 * of attendees initially in replica meetings.
 *
 * Note: all callbacks will be called on main thread.
 */
interface PrimaryMeetingPromotionObserver {
    /**
     * Upon success/failure of [AudioVideoFacade.promoteToPrimaryMeeting] the following observer
     * will return a [MeetingSessionStatus] that will contain a [MeetingSessionStatusCode] of the following:
     *
     * * [MeetingSessionStatusCode.OK]: The promotion was successful (i.e. session token was valid,
     *   there was room in the Primary meeting, etc.), audio will begin flowing
     *   and the attendee can begin to send data messages, and content/video if the call is not already at limit.
     * * [MeetingSessionStatusCode.AudioAuthenticationRejected]: Credentials provided
     *   were invalid when connection attempted to Primary meeting. There may be an issue
     *   with your mechanism which allocates the Primary meeting attendee for the Replica
     *   meeting proxied promotion.  This also may indicate that this API was called in a
     *   non-Replica meeting.
     * * [MeetingSessionStatusCode.AudioCallAtCapacity]: Credentials provided were correct
     *   but there was no room in the Primary meeting.  Promotions to Primary meeting attendee take up a slot, just like
     *   regular Primary meeting attendee connections and are limited by the same mechanisms.
     * * [MeetingSessionStatusCode.AudioServiceUnavailable]: Media has not been connected yet so promotion is not yet possible.
     * * [MeetingSessionStatusCode.AudioInternalServerError]: Other failure, possibly due to disconnect
     *   or timeout. These failures are likely retryable.
     *
     *
     * @param status: [MeetingSessionStatus] - Reason for demotion, see notes above
     */
    fun onPrimaryMeetingPromotion(status: MeetingSessionStatus) {}

    /**
     * This observer callback will only be called for attendees in Replica meetings that have
     * been promoted to the Primary meeting via [AudioVideoFacade.promoteToPrimaryMeeting].
     *
     * Indicates that the client is no longer authenticated to the Primary meeting
     * and can no longer share media. `status` will contain a [MeetingSessionStatusCode] of the following:
     *
     * * [MeetingSessionStatusCode.OK]: [AudioVideoFacade.demoteFromPrimaryMeeting] was used to remove the attendee.
     * * [MeetingSessionStatusCode.AudioAuthenticationRejected]: `chime::DeleteAttendee` was called on the Primary
     *   meeting attendee used in [AudioVideoFacade.promoteToPrimaryMeeting].
     * * [MeetingSessionStatusCode.AudioInternalServerError]: Other failure, possibly due to disconnect
     *   or timeout. These failures are likely retryable. Any disconnection will trigger an automatic
     *   demotion to avoid unexpected or unwanted promotion state on reconnection.
     *
     * @param status: [MeetingSessionStatus] - Reason for demotion, see notes above
     */
    fun onPrimaryMeetingDemotion(status: MeetingSessionStatus) {}
}
