/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.internal.utils.JsonUtils
import org.junit.Assert
import org.junit.Test

class EventAttributesTest {
    @Test
    fun `toJsonString should call JSONUtils marshal function`() {
        val attributes = mutableMapOf<EventAttributeName, Any>()
        val text = attributes.toJsonString()

        Assert.assertEquals(JsonUtils.marshal(attributes), text)
    }
}
