/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdk.meetings.device

/**
 * [DeviceChangeObserver] listens to the change of Audio Device.
 */
interface DeviceChangeObserver {
    /**
     * Called when audio devices are changed.
     *
     * @param freshAudioDeviceList: List<[MediaDevice]> - An updated list of audio devices
     */
    fun onAudioDeviceChange(freshAudioDeviceList: List<MediaDevice>)
}
