/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

/**
 * [ContentShareStatusCode] indicates the reason the content share event occurred.
 */
enum class ContentShareStatusCode {
    /**
     * No failure.
     */
    OK,

    /**
     * Content share video connection is in an unrecoverable failed state.
     * Restart content share connection when this error is encountered.
     */
    VideoServiceFailed;
}
