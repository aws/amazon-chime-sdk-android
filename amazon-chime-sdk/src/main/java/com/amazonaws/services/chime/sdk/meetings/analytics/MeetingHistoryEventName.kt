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
     * The video client WebSocket opened.
     */
    videoClientSignalingOpened,

    /**
     * The video client WebSocket failed or closed with an error.
     */
    videoClientSignalingDropped,

    /**
     * The video client ICE candidate gathering has finished.
     */
    videoClientIceGatheringCompleted,

    /**
     * The content share WebSocket opened.
     */
    contentShareSignalingOpened,

    /**
     * The content share WebSocket failed or closed with an error.
     */
    contentShareSignalingDropped,

    /**
     * The content share ICE candidate gathering has finished.
     */
    contentShareIceGatheringCompleted,

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
    appMemoryLow,

    /**
     * Voice focus enabled
     */
    voiceFocusEnabled,

    /**
     * Voice focus disabled
     */
    voiceFocusDisabled,

    /**
     * Failed to enable voice focus
     */
    voiceFocusEnableFailed,

    /**
     * Failed to disable voice focus
     */
    voiceFocusDisableFailed,

    /**
     * Video capture session interruption began
     */
    videoCaptureSessionInterruptionBegan,

    /**
     * Video capture session interruption ended
     */
    videoCaptureSessionInterruptionEnded,

    /**
     * Network connection type is changed
     */
    networkConnectionTypeChanged;

    companion object {
        fun fromMeetingEvent(name: EventName): MeetingHistoryEventName {
            return when (name) {
                EventName.meetingStartSucceeded -> meetingStartSucceeded
                EventName.meetingReconnected -> meetingReconnected
                EventName.audioInputSelected -> audioInputFailed
                EventName.audioInputFailed -> audioInputFailed
                EventName.videoInputSelected -> videoInputSelected
                EventName.videoInputFailed -> videoInputFailed
                EventName.meetingStartRequested -> meetingStartRequested
                EventName.meetingStartFailed -> meetingStartFailed
                EventName.meetingEnded -> meetingEnded
                EventName.meetingFailed -> meetingFailed
                EventName.videoClientSignalingOpened -> videoClientSignalingOpened
                EventName.videoClientSignalingDropped -> videoClientSignalingDropped
                EventName.videoClientIceGatheringCompleted -> videoClientIceGatheringCompleted
                EventName.contentShareSignalingOpened -> contentShareSignalingOpened
                EventName.contentShareSignalingDropped -> contentShareSignalingDropped
                EventName.contentShareIceGatheringCompleted -> contentShareIceGatheringCompleted
                EventName.contentShareStartRequested -> contentShareStartRequested
                EventName.contentShareStarted -> contentShareStarted
                EventName.contentShareStopped -> contentShareStopped
                EventName.contentShareFailed -> contentShareFailed
                EventName.appStateChanged -> appStateChanged
                EventName.appMemoryLow -> appMemoryLow
                EventName.voiceFocusEnabled -> voiceFocusEnabled
                EventName.voiceFocusDisabled -> voiceFocusDisabled
                EventName.voiceFocusEnableFailed -> voiceFocusEnableFailed
                EventName.voiceFocusDisableFailed -> voiceFocusDisableFailed
                EventName.videoCaptureSessionInterruptionBegan -> videoCaptureSessionInterruptionBegan
                EventName.videoCaptureSessionInterruptionEnded -> videoCaptureSessionInterruptionEnded
                EventName.networkConnectionTypeChanged -> networkConnectionTypeChanged
            }
        }
    }
}
