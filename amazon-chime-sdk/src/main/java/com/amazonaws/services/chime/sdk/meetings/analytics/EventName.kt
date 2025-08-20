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
     * The microphone selection or access failed.
     */
    audioInputFailed,

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
     * The WebSocket failed or closed with an error.
     */
    signalingDropped,

    /**
     * The meeting ended with failure.
     */
    meetingFailed
}
