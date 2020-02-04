package com.amazon.chime.sdk.media.devicecontroller

/**
 * Media device with its info.
 *
 * @property label human readable string describing the device.
 * @property type media device type e.g. 2 (Build-in speaker)
 */
data class MediaDevice(val label: String, val type: Int) {
    override fun toString(): String = label
}
