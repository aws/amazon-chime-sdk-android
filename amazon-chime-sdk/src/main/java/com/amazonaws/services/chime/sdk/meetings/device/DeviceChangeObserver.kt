/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

/**
 * [DeviceChangeObserver] listens audio device changes.
 *
 * Note: all callbacks will be called on main thread.
 */
interface DeviceChangeObserver {
    /**
     * Called when audio devices are changed.
     *
     * Note: this callback will be called on main thread.
     *
     * @param freshAudioDeviceList: List<[MediaDevice]> - An updated list of audio devices.
     */
    fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>)
}
