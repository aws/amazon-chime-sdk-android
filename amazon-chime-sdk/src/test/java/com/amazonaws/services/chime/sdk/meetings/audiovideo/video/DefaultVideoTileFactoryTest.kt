/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
        val testHeight = 1280
        val testWidth = 720
        val testLogger = ConsoleLogger()
        val videoTileFactory = DefaultVideoTileFactory(testLogger)

        val testOutput: VideoTile = videoTileFactory.makeTile(testTileId, testAttendeeId, testHeight, testWidth)

        assertNotNull(testOutput)
        assertEquals(testTileId, testOutput.state.tileId)
        assertEquals(testAttendeeId, testOutput.state.attendeeId)
        assertEquals(testHeight, testOutput.state.videoStreamContentHeight)
        assertEquals(testWidth, testOutput.state.videoStreamContentWidth)
    }
}
