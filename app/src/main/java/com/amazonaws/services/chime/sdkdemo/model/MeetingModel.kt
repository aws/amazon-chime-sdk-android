/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.ViewModel
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdkdemo.data.Message
import com.amazonaws.services.chime.sdkdemo.data.MetricData
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.data.VideoCollectionTile

// This will be used for keeping state after rotation
class MeetingModel : ViewModel() {
    val currentMetrics = mutableMapOf<String, MetricData>()
    val currentRoster = mutableMapOf<String, RosterAttendee>()
    val currentVideoTiles = mutableMapOf<Int, VideoCollectionTile>()
    val currentScreenTiles = mutableMapOf<Int, VideoCollectionTile>()
    val nextVideoTiles = LinkedHashMap<Int, VideoCollectionTile>()
    var currentMediaDevices = listOf<MediaDevice>()
    var currentMessages = mutableListOf<Message>()

    var isMuted = false
    // Voice Focus is started in the beginning of the call so init to true
    var isVoiceFocusOn = true
    var isCameraOn = false
    var isDeviceListDialogOn = false
    var lastReceivedMessageTimestamp = 0L
    var tabIndex = 0
}
