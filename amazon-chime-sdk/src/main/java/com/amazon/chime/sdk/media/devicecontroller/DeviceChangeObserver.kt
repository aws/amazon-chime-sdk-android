/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.devicecontroller

interface DeviceChangeObserver {
    /**
     *Called when audio devices are changed.
     */
    fun onAudioDeviceChange(freshAudioDeviceList: List<MediaDevice>)
}
