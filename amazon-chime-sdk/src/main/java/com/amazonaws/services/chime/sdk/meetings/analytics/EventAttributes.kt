/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.internal.utils.JsonUtils

/**
 * [EventAttributes] describes meeting event.
 */
typealias EventAttributes = MutableMap<EventAttributeName, Any>

/**
 * Convert event attributes into JSON string.
 *
 * @return string
 */
fun EventAttributes.toJsonString(): String {
    return JsonUtils.marshal(this)
}
