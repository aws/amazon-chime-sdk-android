/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.metric

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservableMetricTest {
    @Test
    fun `isContentShareMetric should return expected values`() {
        assertTrue(ObservableMetric.contentShareVideoSendBitrate.isContentShareMetric())
        assertFalse(ObservableMetric.videoSendBitrate.isContentShareMetric())
        assertTrue(ObservableMetric.contentShareVideoSendFps.isContentShareMetric())
        assertFalse(ObservableMetric.videoSendFps.isContentShareMetric())
        assertTrue(ObservableMetric.contentShareVideoSendPacketLossPercent.isContentShareMetric())
        assertFalse(ObservableMetric.videoSendPacketLossPercent.isContentShareMetric())
        assertTrue(ObservableMetric.contentShareVideoSendRttMs.isContentShareMetric())
        assertFalse(ObservableMetric.videoSendRttMs.isContentShareMetric())
    }
}
