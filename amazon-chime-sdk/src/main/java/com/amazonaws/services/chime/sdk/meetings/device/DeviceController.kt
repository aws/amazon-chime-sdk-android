/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.os.Build
import androidx.annotation.RequiresApi
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoControllerFacade

/**
 * [DeviceController] keeps track of the devices being used for audio device
 * (e.g. built-in speaker), video input (e.g. camera)).
 * The list functions return [MediaDevice] objects.
 * Changes in device availability are broadcast to any registered
 * [DeviceChangeObserver].
 */
interface DeviceController {
    /**
     * Lists currently available audio devices.
     *
     * Note: If there are both USB and earphone jack connected. The device will only show earphone.
     *
     * @return a list of currently available audio devices.
     */
    fun listAudioDevices(): List<MediaDevice>

    /**
     * Selects an audio device to use.
     *
     * Note: [chooseAudioDevice] is no-op when audio client is not started.
     *
     * @param mediaDevice the audio device selected to use.
     */
    fun chooseAudioDevice(mediaDevice: MediaDevice)

    /**
     * Get the active input/output audio device in the meeting, return null if there isn't any.
     *
     * NOTE: This requires Android API 24 and above
     *
     * @return the active local audio device
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun getActiveAudioDevice(): MediaDevice?

    /**
     * Adds an observer to receive callbacks about device changes.
     *
     * @param observer device change observer
     */
    fun addDeviceChangeObserver(observer: DeviceChangeObserver)

    /**
     * Removes an observer to stop receiving callbacks about device changes.
     *
     * @param observer device change observer
     */
    fun removeDeviceChangeObserver(observer: DeviceChangeObserver)

    /**
     * Get the currently active camera, if any. This will return null if using a custom source,
     * e.g. one passed in via [AudioVideoControllerFacade.startLocalVideo]
     *
     * @return [MediaDevice] - Information about the current active device used for video.
     */
    fun getActiveCamera(): MediaDevice?

    /**
     * Switches the currently active camera. This will no-op if using a custom source,
     * e.g. one passed in via [AudioVideoControllerFacade.startLocalVideo]
     */
    fun switchCamera()
}
