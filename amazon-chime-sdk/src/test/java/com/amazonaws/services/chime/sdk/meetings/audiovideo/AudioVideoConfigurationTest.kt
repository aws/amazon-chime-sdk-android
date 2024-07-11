package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioDeviceCapabilities
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioMode
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioRecordingPresetOverride
import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.AudioStreamType
import org.junit.Assert
import org.junit.Test

class AudioVideoConfigurationTest {
    @Test
    fun `default audio mode should be stereo`() {
        Assert.assertEquals(AudioVideoConfiguration().audioMode, AudioMode.Stereo48K)
    }

    @Test
    fun `default audio device capabilities should be input and output`() {
        Assert.assertEquals(AudioVideoConfiguration().audioDeviceCapabilities, AudioDeviceCapabilities.InputAndOutput)
    }

    @Test
    fun `default audio stream type should be voice call`() {
        Assert.assertEquals(AudioVideoConfiguration().audioStreamType, AudioStreamType.VoiceCall)
    }

    @Test
    fun `default audio recording preset override should be none`() {
        Assert.assertEquals(AudioVideoConfiguration().audioRecordingPresetOverride, AudioRecordingPresetOverride.None)
    }

    @Test
    fun `audio redundancy should be enabled by default`() {
        Assert.assertEquals(AudioVideoConfiguration().enableAudioRedundancy, true)
    }

    @Test
    fun `default reconnectTimeoutMs should be 180000`() {
        Assert.assertEquals(AudioVideoConfiguration().reconnectTimeoutMs, 180000)
    }
}
