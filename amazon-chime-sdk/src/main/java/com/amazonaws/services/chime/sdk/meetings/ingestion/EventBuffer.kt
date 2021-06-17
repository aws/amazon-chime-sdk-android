/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent

/**
 * [EventBuffer] defines a buffer which will consume the [SDKEvent] internally.
 */
interface EventBuffer {
    /**
     * Add a meeting event to the buffer.
     *
     * @param item: [SDKEvent] - meeting event
     */
    fun add(item: SDKEvent)

    /**
     * Consume the data.
     */
    fun process()
}
