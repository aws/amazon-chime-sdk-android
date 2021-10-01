/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

interface TranscriptEvent

data class Transcript(val results: List<TranscriptResult>) : TranscriptEvent

data class TranscriptResult(val resultId: String, val channelId: String, val isPartial: Boolean,
                            val startTimeMs: Long, val endTimeMs: Long,
                            val alternatives: List<TranscriptAlternative>)

data class TranscriptAlternative(val items: List<TranscriptItem>, val transcript: String)

data class TranscriptItem(val type: TranscriptItemType, val startTimeMs: Long,
                          val endTimeMs: Long, val attendee: AttendeeInfo,
                          val content: String, val vocabularyFilterMatch: Boolean)

data class TranscriptionStatus(val type: TranscriptionStatusType, val eventTimeMs: Long,
                               val transcriptionRegion: String, val transcriptionConfiguration: String,
                               val message: String) : TranscriptEvent

enum class TranscriptItemType(val value: Int) {
    TranscriptItemTypePronunciation(1),
    TranscriptItemTypePunctuation(2);

    companion object {
        fun from(intValue: Int): TranscriptItemType? = values().find { it.value == intValue }
    }
}

enum class TranscriptionStatusType(val value: Int) {
    TranscriptionStatusTypeStarted(1),
    TranscriptionStatusTypeInterrupted(2),
    TranscriptionStatusTypeResumed(3),
    TranscriptionStatusTypeStopped(4),
    TranscriptionStatusTypeFailed(5);

    companion object {
        fun from(intValue: Int): TranscriptionStatusType? = values().find { it.value == intValue }
    }
}
