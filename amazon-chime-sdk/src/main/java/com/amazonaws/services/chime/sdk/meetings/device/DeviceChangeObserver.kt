/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

/**
 * [DeviceChangeObserver] listens audio device changes.
 */
interface DeviceChangeObserver {
    /**
     * Called when audio devices are changed.
     *
     * @param freshAudioDeviceList: List<[MediaDevice]> - An updated list of audio devices.
     */
    fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>)
}
