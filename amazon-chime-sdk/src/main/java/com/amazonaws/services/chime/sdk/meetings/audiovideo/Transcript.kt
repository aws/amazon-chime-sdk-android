/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

interface TranscriptEvent

data class Transcript(val results: Array<TranscriptResult>) : TranscriptEvent

data class TranscriptResult(val resultId: String, val channelId: String, val isPartial: Boolean,
                            val startTimeMs: Long, val endTimeMs: Long,
                            val alternatives: Array<TranscriptAlternative>)

data class TranscriptAlternative(val items: Array<TranscriptItem>, val transcript: String)

data class TranscriptItem(val type: TranscriptItemType, val startTimeMs: Long,
                          val endTimeMs: Long, val attendee: AttendeeInfo,
                          val content: String, val vocabularyFilterMatch: Boolean)

data class TranscriptionStatus(val type: TranscriptionStatusType, val eventTimeMs: Long,
                               val transcriptionRegion: String, val transcriptionConfiguration: String,
                               val message: String) : TranscriptEvent

enum class TranscriptItemType(val value: Int) {
    Unknown(0),
    Pronunciation(1),
    Punctuation(2);

    companion object {
        fun from(intValue: Int): TranscriptItemType {
            return values().find { it.value == intValue } ?: return Unknown
        }
    }
}

enum class TranscriptionStatusType(val value: Int) {
    Unknown(0),
    Started(1),
    Interrupted(2),
    Resumed(3),
    Stopped(4),
    Failed(5);

    companion object {
        fun from(intValue: Int): TranscriptionStatusType {
            return values().find { it.value == intValue } ?: return Unknown
        }
    }
}
