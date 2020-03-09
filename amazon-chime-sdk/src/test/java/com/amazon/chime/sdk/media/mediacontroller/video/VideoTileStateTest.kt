package com.amazon.chime.sdk.media.mediacontroller.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTileStateTest {

    private val tileId = 117 // some random id
    private val attendeeIdVideo = "chimesarang"
    private val attendeeIdScreenShare = "chimesarang#content"

    // local tile
    val vtsLocal = VideoTileState(tileId, null, false)

    // regular video sharing
    val vtsVideo = VideoTileState(tileId, attendeeIdVideo, false)

    // screen sharing
    val vtsScreenShare = VideoTileState(tileId, attendeeIdScreenShare, false)

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
