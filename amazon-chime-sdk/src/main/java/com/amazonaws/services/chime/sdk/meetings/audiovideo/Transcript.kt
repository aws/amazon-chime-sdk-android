/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

interface TranscriptEvent

/*
 * See [Using Amazon Chime SDK live transcription developer guide](https://docs.aws.amazon.com/chime/latest/dg/process-msgs.html) for details about transcription message types and data guidelines
 */
data class Transcript(val results: Array<TranscriptResult>) : TranscriptEvent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transcript

        if (!results.contentEquals(other.results)) return false

        return true
    }

    override fun hashCode(): Int {
        return results.contentHashCode()
    }
}

data class TranscriptResult(
    val resultId: String,
    val channelId: String,
    val isPartial: Boolean,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val alternatives: Array<TranscriptAlternative>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranscriptResult

        if (!alternatives.contentEquals(other.alternatives)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resultId.hashCode()
        result = 31 * result + channelId.hashCode()
        result = 31 * result + isPartial.hashCode()
        result = 31 * result + startTimeMs.hashCode()
        result = 31 * result + endTimeMs.hashCode()
        result = 31 * result + alternatives.contentHashCode()
        return result
    }
}

data class TranscriptAlternative(
    val items: Array<TranscriptItem>,
    val transcript: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranscriptAlternative

        if (!items.contentEquals(other.items)) return false
        if (transcript != other.transcript) return false

        return true
    }

    override fun hashCode(): Int {
        var result = items.contentHashCode()
        result = 31 * result + transcript.hashCode()
        return result
    }
}

data class TranscriptItem(
    val type: TranscriptItemType,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val attendee: AttendeeInfo,
    val content: String,
    val vocabularyFilterMatch: Boolean
)

data class TranscriptionStatus(
    val type: TranscriptionStatusType,
    val eventTimeMs: Long,
    val transcriptionRegion: String,
    val transcriptionConfiguration: String,
    val message: String
) : TranscriptEvent

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
