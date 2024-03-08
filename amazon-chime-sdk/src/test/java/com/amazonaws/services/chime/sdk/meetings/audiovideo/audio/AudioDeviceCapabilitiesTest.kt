package com.amazonaws.services.chime.sdk.meetings.audiovideo.audio

import org.junit.Assert
import org.junit.Test

class AudioDeviceCapabilitiesTest {
    @Test
    fun `get enum value from int`() {
        Assert.assertEquals(AudioDeviceCapabilities.from(0), AudioDeviceCapabilities.None)
        Assert.assertEquals(AudioDeviceCapabilities.from(1), AudioDeviceCapabilities.OutputOnly)
        Assert.assertEquals(AudioDeviceCapabilities.from(2), AudioDeviceCapabilities.InputAndOutput)
    }

    @Test
    fun `get enum value from invalid int returns null`() {
        Assert.assertNull(AudioDeviceCapabilities.from(-1))
    }

    @Test
    fun `get enum value from int with fallback to default value`() {
        Assert.assertEquals(AudioDeviceCapabilities.from(-1, AudioDeviceCapabilities.InputAndOutput), AudioDeviceCapabilities.InputAndOutput)
    }
}
