package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

import org.junit.Assert
import org.junit.Test

class AudioModeTest {
    @Test
    fun `get enum value from int`() {
        Assert.assertEquals(AudioMode.from(0), AudioMode.NoAudio)
        Assert.assertEquals(AudioMode.from(1), AudioMode.Mono)
    }

    @Test
    fun `get enum value from invalid int returns null`() {
        Assert.assertNull(AudioMode.from(-1))
    }

    @Test
    fun `get enum value from int with fallback to default value`() {
        Assert.assertEquals(AudioMode.from(-1, AudioMode.Mono), AudioMode.Mono)
    }
}
