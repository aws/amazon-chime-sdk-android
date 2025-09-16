/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

/**
 * [EventName] represent sdk event that could help builders to analyze the data.
 */
enum class EventName {
    /**
     * The microphone was selected.
     */
    audioInputSelected,

    /**
     * The microphone selection or access failed.
     */
    audioInputFailed,

    /**
     * The camera was selected.
     */
    videoInputSelected,

    /**
     * The camera selection or access failed.
     */
    videoInputFailed,

    /**
     * The meeting will start.
     */
    meetingStartRequested,

    /**
     * The meeting started.
     */
    meetingStartSucceeded,

    /**
     * The meeting reconnected.
     */
    meetingReconnected,

    /**
     * The meeting failed to start.
     */
    meetingStartFailed,

    /**
     * The meeting ended.
     */
    meetingEnded,

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
     * The meeting ended with failure.
     */
    meetingFailed,

    /**
     * The application state changed.
     */
    appStateChanged,

    /**
     * The application received a memory low warning.
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
}
