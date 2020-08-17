/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice

class DeviceAdapter(
    context: Context,
    resource: Int,
    private val devices: List<MediaDevice>
) :
    ArrayAdapter<MediaDevice>(context, resource, devices) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.contentDescription = devices[position].type.name
        return view
    }
}
