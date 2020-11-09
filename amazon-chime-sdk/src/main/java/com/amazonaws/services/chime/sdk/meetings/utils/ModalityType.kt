/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

enum class ModalityType(val value: String) {
    Content("content");

    companion object {
        fun fromValue(value: String): ModalityType? = values().find { it.value == value }
    }
}
