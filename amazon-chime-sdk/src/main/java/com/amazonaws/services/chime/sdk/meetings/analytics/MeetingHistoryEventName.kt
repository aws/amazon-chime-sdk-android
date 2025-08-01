/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [MeetingHistoryEventName] is a notable event (such as MeetingStartSucceeded) that occur during meeting.
 * Thus, this also includes events in [EventName].
 */
enum class MeetingHistoryEventName {
    /**
     * The microphone was selected.
     */
    audioInputSelected,

    /**
     * The microphone selection failed.
     */
    audioInputFailed,

    /**
     * The camera selection failed.
     */
    videoInputFailed,

    /**
     * The camera was selected.
     */
    videoInputSelected,

    /**
     * The microphone or camera device access failed.
     */
    deviceAccessFailed,

    /**
     * The meeting failed to start.
     */
    meetingStartFailed,

    /**
     * The meeting will start.
     */
    meetingStartRequested,

    /**
     * The meeting started.
     */
    meetingStartSucceeded,

    /**
     * The meeting ended.
     */
    meetingEnded,

    /**
     * The meeting failed.
     */
    meetingFailed,

    /**
     * The meeting reconnected.
     */
    meetingReconnected;

    companion object {
        fun fromMeetingEvent(name: EventName): MeetingHistoryEventName {
            return when (name) {
                EventName.meetingStartSucceeded -> meetingStartSucceeded
                EventName.meetingReconnected -> meetingReconnected
                EventName.audioInputFailed -> audioInputFailed
                EventName.videoInputFailed -> videoInputFailed
                EventName.deviceAccessFailed -> deviceAccessFailed
                EventName.meetingStartRequested -> meetingStartRequested
                EventName.meetingStartFailed -> meetingStartFailed
                EventName.meetingEnded -> meetingEnded
                EventName.meetingFailed -> meetingFailed
            }
        }
    }
}
