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
     * The microphone selection or access failed.
     */
    audioInputFailed,

    /**
     * The camera selection or access failed.
     */
    videoInputFailed,

    /**
     * The camera was selected.
     */
    videoInputSelected,

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
    meetingReconnected,

    /**
     * The video client WebSocket failed or closed with an error.
     */
    videoClientSignalingDropped,

    /**
     * The content share WebSocket failed or closed with an error.
     */
    contentShareSignalingDropped,

    /**
     * The content share start was requested.
     */
    contentShareStartRequested,

    /**
     * The content share started successfully.
     */
    contentShareStarted,

    /**
     * The content share stopped.
     */
    contentShareStopped,

    /*
     * The content share failed to start.
     */
    contentShareFailed,

    /**
     * The application state is changed.
     */
    appStateChanged,

    /**
     * The application memory is low.
     */
    appMemoryLow;

    companion object {
        fun fromMeetingEvent(name: EventName): MeetingHistoryEventName {
            return when (name) {
                EventName.meetingStartSucceeded -> meetingStartSucceeded
                EventName.meetingReconnected -> meetingReconnected
                EventName.audioInputFailed -> audioInputFailed
                EventName.videoInputFailed -> videoInputFailed
                EventName.meetingStartRequested -> meetingStartRequested
                EventName.meetingStartFailed -> meetingStartFailed
                EventName.meetingEnded -> meetingEnded
                EventName.meetingFailed -> meetingFailed
                EventName.videoClientSignalingDropped -> videoClientSignalingDropped
                EventName.contentShareSignalingDropped -> contentShareSignalingDropped
                EventName.contentShareStartRequested -> contentShareStartRequested
                EventName.contentShareStarted -> contentShareStarted
                EventName.contentShareStopped -> contentShareStopped
                EventName.contentShareFailed -> contentShareFailed
                EventName.appStateChanged -> appStateChanged
                EventName.appMemoryLow -> appMemoryLow
            }
        }
    }
}
