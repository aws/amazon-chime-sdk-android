/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.realtime

import com.amazonaws.services.chime.sdk.meetings.audiovideo.TranscriptEvent

/**
 * [TranscriptEventObserver] lets one listen to [TranscriptEvent] events of current meeting
 *
 * Note: all callbacks will be called on main thread.
 */
interface TranscriptEventObserver {
    /**
     * Handles receive of transcript event - [TranscriptEvent].
     *
     * Note: this callback will be called on main thread.
     *
     * @param transcriptEvent: Array<[TranscriptEvent]> - Transcript events.
     */
    fun onTranscriptEventReceived(transcriptEvent: TranscriptEvent)
}
