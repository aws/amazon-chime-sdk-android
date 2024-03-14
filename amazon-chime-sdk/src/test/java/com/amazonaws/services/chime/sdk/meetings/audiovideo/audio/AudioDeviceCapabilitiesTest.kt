package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

import android.Manifest
import org.junit.Test

class AudioDeviceCapabilitiesTest {
    @Test
    fun `should not require permissions for None`() {
        assert(AudioDeviceCapabilities.None.requiredPermissions().isEmpty())
    }

    @Test
    fun `should require MODIFY_AUDIO_SETTINGS permissions for OutputOnly`() {
        assert(AudioDeviceCapabilities.OutputOnly.requiredPermissions().contains(Manifest.permission.MODIFY_AUDIO_SETTINGS))
    }

    @Test
    fun `should check MODIFY_AUDIO_SETTINGS and RECORD_AUDIO permissions for InputAndOutput`() {
        assert(AudioDeviceCapabilities.InputAndOutput.requiredPermissions().contains(Manifest.permission.MODIFY_AUDIO_SETTINGS))
        assert(AudioDeviceCapabilities.InputAndOutput.requiredPermissions().contains(Manifest.permission.RECORD_AUDIO))
    }
}
