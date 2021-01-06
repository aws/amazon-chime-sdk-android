/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

/**
 * [DefaultModality] is a backwards compatible extension of the
 * attendee id (UUID string) and session token schemas (base 64 string).
 * It appends #<modality> to either strings, which indicates the modality
 * of the participant.
 *
 * For example,
 * attendeeId: "abcdefg"
 * contentAttendeeId: "abcdefg#content"
 * base(contentAttendeeId): "abcdefg"
 * modality(contentAttendeeId): "content"
 * hasModality(contentAttendeeId): true
 */
class DefaultModality(private val id: String) {

    companion object {
        const val MODALITY_SEPARATOR = "#"
    }

    /**
     * The Id
     */
    fun id(): String {
        return id
    }

    /**
     * The base of the Id
     */
    fun base(): String {
        if (id.isEmpty()) {
            return ""
        }
        return id.split(MODALITY_SEPARATOR)[0]
    }

    /**
     * The modality of the Id
     */
    fun modality(): ModalityType? {
        if (id.isEmpty()) {
            return null
        }
        val components = id.split(MODALITY_SEPARATOR)
        return if (components.size == 2) ModalityType.fromValue(components[1]) else null
    }

    /**
     * Check whether the current Id contains the input modality
     */
    fun hasModality(modality: ModalityType): Boolean {
        return modality() == modality
    }
}
