/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

/**
 * [MeetingSessionStatusCode] provides additional details for the [MeetingSessionStatus]
 * received for a session.
 */
enum class MeetingSessionStatusCode(val value: Int) {
    /**
     * Everything is OK so far.
     */
    OK(0),

    /**
     * The attendee left the meeting normally.
     */
    Left(1),

    /**
     * The attendee joined from another device.
     */
    AudioJoinedFromAnotherDevice(2),

    /**
     * The attendee should explicitly switch itself from joined with audio to
     * checked-in.
     */
    AudioDisconnectAudio(3),

    /**
     * Authentication was rejected as the attendee information in MeetingSessionCredentials did not
     * match that of an attendee created via chime::CreateAttendee.
     * This error may imply an issue with your credential providing service,
     * the client will not be allowed on this call.
     */
    AudioAuthenticationRejected(4),

    /**
     * The client can not join because the meeting is at capacity.
     * The service supports up to 250 attendees.
     */
    AudioCallAtCapacity(5),

    /**
     * The attendee attempted to join a meeting that has already ended.
     * See this [FAQ](https://aws.github.io/amazon-chime-sdk-js/modules/faqs.html#when-does-an-amazon-chime-sdk-meeting-end)
     * for more information. The end user may want to be notified of this type of error.
     */
    AudioCallEnded(6),

    /**
     * There was an internal server error related to audio.
     * This may indicate some issue with the audio device,
     * or an issue with the Amazon Chime SDK service itself
     */
    AudioInternalServerError(7),

    /**
     * There was an internal server error related to audio.
     * This may indicate some issue with the audio device,
     * or an issue with the Amazon Chime SDK service itself.
     */
    AudioServiceUnavailable(8),

    /**
     * There was an internal server error related to audio.
     * This may indicate some issue with the audio device,
     * or an issue with the Amazon Chime SDK service itself.
     */
    AudioDisconnected(9),

    /**
     * Due to connection health, a reconnect has been triggered.
     */
    ConnectionHealthReconnect(10),

    /**
     * Network is not good enough for VoIP, `AudioVideoObserver.audioSessionDidDrop()` will be
     * triggered, and there will an automatic attempt of reconnecting.
     * If the reconnecting is successful, `onAudioSessionStarted` will be called with value of
     * reconnecting as true.
     */
    NetworkBecamePoor(11),

    /**
     * There was an internal server error related to video. This may indicate some issue with the
     * video device, or an issue with the Amazon Chime SDK service itself.
     */
    VideoServiceFailed(12),

    /**
     * The video client has tried to send video but was unable to do so due to capacity reached.
     * However, the video client can still receive remote video streams.
     */
    VideoAtCapacityViewOnly(13),

    /**
     * Designated output device is not responding and timed out.
     */
    AudioOutputDeviceNotResponding(14),

    /**
     * Designated input device is not responding and timed out.
     */
    AudioInputDeviceNotResponding(15);

    companion object {
        fun from(intValue: Int): MeetingSessionStatusCode? = values().find { it.value == intValue }
    }
}
