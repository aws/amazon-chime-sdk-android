/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdkdemo.device.AudioDeviceManager

internal const val AUDIO_RECORDING_CONFIG_API_LEVEL = 24

class DeviceAdapter(
    context: Context,
    resource: Int,
    private val devices: List<MediaDevice>,
    private val audioVideo: AudioVideoFacade,
    private val audioManager: AudioDeviceManager
) :
    ArrayAdapter<MediaDevice>(context, resource, devices) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.contentDescription = devices[position].type.name
        if (Build.VERSION.SDK_INT >= AUDIO_RECORDING_CONFIG_API_LEVEL) {
            val currentDevice = audioVideo.getActiveAudioDevice()
            view.text =
                if (currentDevice == devices[position]) "${devices[position]} ✓" else devices[position].toString()
        } else {
            val currentDevice = audioManager.activeAudioDevice
            view.text =
                if (currentDevice == devices[position]) "${devices[position]} ✓" else devices[position].toString()
        }
        return view
    }
}
