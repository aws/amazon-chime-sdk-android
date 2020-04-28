/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import kotlinx.android.synthetic.main.roster_view_attendee_row.view.attendeeName
import kotlinx.android.synthetic.main.roster_view_attendee_row.view.attendeeVolume

class RosterAdapter(
    private val roster: MutableCollection<RosterAttendee>
) :
    RecyclerView.Adapter<RosterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RosterHolder {
        val inflatedView = parent.inflate(R.layout.roster_view_attendee_row, false)
        return RosterHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return roster.size
    }

    override fun onBindViewHolder(holder: RosterHolder, position: Int) {
        val attendee = roster.elementAt(position)
        holder.bindAttendee(attendee)
    }
}

class RosterHolder(inflatedView: View) :
    RecyclerView.ViewHolder(inflatedView) {

    private var view: View = inflatedView
    private var attendeeName: String? = null

    fun bindAttendee(attendee: RosterAttendee) {
        this.attendeeName = attendee.attendeeName
        view.attendeeName.text = attendeeName
        view.attendeeName.contentDescription = attendeeName

        if (attendee.signalStrength == SignalStrength.None ||
            attendee.signalStrength == SignalStrength.Low) {
            val drawable = if (attendee.volumeLevel == VolumeLevel.Muted) {
                R.drawable.ic_microphone_poor_connectivity_dissabled
            } else {
                R.drawable.ic_microphone_poor_connectivity
            }
            view.attendeeVolume.setImageResource(drawable)
            view.contentDescription = "$attendeeName Signal Strength Poor"
        } else {
            when (attendee.volumeLevel) {
                VolumeLevel.Muted -> {
                    view.attendeeVolume.setImageResource(R.drawable.ic_microphone_disabled)
                    view.attendeeVolume.contentDescription = "$attendeeName Muted"
                }
                VolumeLevel.NotSpeaking -> {
                    view.attendeeVolume.setImageResource(R.drawable.ic_microphone_enabled)
                    view.attendeeVolume.contentDescription = "$attendeeName Not Speaking"
                }
                VolumeLevel.Low -> {
                    view.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_1)
                    view.attendeeVolume.contentDescription = "$attendeeName Speaking"
                }
                VolumeLevel.Medium -> {
                    view.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_2)
                    view.attendeeVolume.contentDescription = "$attendeeName Speaking"
                }
                VolumeLevel.High -> {
                    view.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_3)
                    view.attendeeVolume.contentDescription = "$attendeeName Speaking"
                }
            }
        }
    }
}
