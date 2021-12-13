package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

import org.junit.Assert
import org.junit.Test

class AudioModeTest {
    @Test
    fun `get enum value from int`() {
        Assert.assertEquals(AudioMode.from(1), AudioMode.Mono16K)
        Assert.assertEquals(AudioMode.from(2), AudioMode.Mono48K)
        Assert.assertEquals(AudioMode.from(3), AudioMode.Stereo48K)
    }

    @Test
    fun `get enum value from invalid int returns null`() {
        Assert.assertNull(AudioMode.from(-1))
    }

    @Test
    fun `get enum value from int with fallback to default value`() {
        Assert.assertEquals(AudioMode.from(-1, AudioMode.Stereo48K), AudioMode.Stereo48K)
    }
}
