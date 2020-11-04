/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DefaultVideoTileFactoryTest {
    @Test
    fun `makeTile should return object with data from parameters`() {
        val testTileId = 1
        val testAttendeeId = "attendeeId"
        val testWidth = 720
        val testHeight = 1280
        val testLogger = ConsoleLogger()
        val videoTileFactory = DefaultVideoTileFactory(testLogger)

        val testOutput: VideoTile = videoTileFactory.makeTile(testTileId, testAttendeeId, testWidth, testHeight, false)

        assertNotNull(testOutput)
        assertEquals(testTileId, testOutput.state.tileId)
        assertEquals(testAttendeeId, testOutput.state.attendeeId)
        assertEquals(testWidth, testOutput.state.videoStreamContentWidth)
        assertEquals(testHeight, testOutput.state.videoStreamContentHeight)
    }
}
