/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributes

/**
 * A record that contains batch of [IngestionEvent] to send.
 * This contains metadata that is shared among events.
 * @property metadata
 * @property events
 *
 * This will have format of following
 * Payload will look like
 * {
 *   "metadata": {
 *        "deviceName": "samsung SM-G960U",
 *        "deviceManufacturer": "samsung",
 *        "deviceModel": "SM-G960U",
 *        "mediaSdkVersion": "0.11.2",
 *        "osName": "Android",
 *        "meetingId": "eb783287-94aa-43c4-9a73-9ead14e697a5",
 *        "attendeeId": "c2435dfb-0c70-4152-be18-efc8773b8b2b"
 *        "osVersion": "10",
 *        "sdkName": "amazon-chime-sdk-android",
 *        "sdkVersion": "0.11.4"
 *    }
 *  "events": [
 *    {
 *      "type": "Meet",
 *      "v": 1,
 *      "payloads": [
 *        {
 *          "ts": 78645356720,
 *          "name": "meetingStartSucceeded",
 *          "maxVideoTileCount": 0,
 *          "poorConnectionCount": 0,
 *          "retryCount": 0,
 *          "signalingOpenDurationMs": 666
 *        },
 *        {
 *          "ts": 78645356797,
 *          "name": "meetingEnded",
 *          "maxVideoTileCount": 0,
 *          "poorConnectionCount": 0,
 *          "retryCount": 0,
 *          "signalingOpenDurationMs": 444
 *        }
 *      ]
 *    }
 *  ]
 * }
 */
typealias IngestionMetadata = EventAttributes

data class IngestionRecord(val metadata: IngestionMetadata, val events: List<IngestionEvent>)
