/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils

// [PermissionError]represents different types of permission errors
enum class PermissionError {
    /**
     * Audio permission was denied or not granted
     */
    AudioPermissionError,

    /**
     * Video permission was denied or not granted
     */
    VideoPermissionError;
}
