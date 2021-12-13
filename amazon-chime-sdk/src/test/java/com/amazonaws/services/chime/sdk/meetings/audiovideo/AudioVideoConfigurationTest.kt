package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import org.junit.Assert
import org.junit.Test

class AudioVideoConfigurationTest {
    @Test
    fun `default audio mode should be stereo`() {
        Assert.assertEquals(AudioVideoConfiguration().audioMode, AudioMode.Stereo48K)
    }
}
