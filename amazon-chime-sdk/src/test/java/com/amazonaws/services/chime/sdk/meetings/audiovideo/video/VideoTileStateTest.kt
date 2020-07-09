package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTileStateTest {

    private val tileId = 117 // some random id
    private val testHeight = 1280
    private val testWidth = 720
    private val attendeeIdVideo = "chimesarang"
    private val attendeeIdScreenShare = "chimesarang#content"

    // local tile
    val vtsLocal = VideoTileState(tileId, null, testHeight, testWidth, VideoPauseState.Unpaused)

    // regular video sharing
    val vtsVideo = VideoTileState(tileId, attendeeIdVideo, testHeight, testWidth, VideoPauseState.Unpaused)

    // screen sharing
    val vtsScreenShare = VideoTileState(tileId, attendeeIdScreenShare, testHeight, testWidth, VideoPauseState.Unpaused)

    @Test
    fun `isLocalTile should be true when tile is local`() {
        assertTrue(vtsLocal.isLocalTile)
        assertFalse(vtsVideo.isLocalTile)
        assertFalse(vtsScreenShare.isLocalTile)
    }

    @Test
    fun `isContent should be true when sharing screen`() {
        assertFalse(vtsLocal.isContent)
        assertFalse(vtsVideo.isContent)
        assertTrue(vtsScreenShare.isContent)
    }
}
