/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.utils.inflate
import kotlinx.android.synthetic.main.row_roster.view.activeSpeakerIndicator
import kotlinx.android.synthetic.main.row_roster.view.attendeeName
import kotlinx.android.synthetic.main.row_roster.view.attendeeVolume

class RosterAdapter(
    private val roster: Collection<RosterAttendee>
) :
    RecyclerView.Adapter<RosterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RosterHolder {
        val inflatedView = parent.inflate(R.layout.row_roster, false)
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

    fun bindAttendee(attendee: RosterAttendee) {
        val attendeeName = attendee.attendeeName
        view.attendeeName.text = attendeeName
        view.attendeeName.contentDescription = attendeeName
        view.activeSpeakerIndicator.visibility = if (attendee.isActiveSpeaker) View.VISIBLE else View.INVISIBLE
        view.activeSpeakerIndicator.contentDescription = if (attendee.isActiveSpeaker) "${attendee.attendeeName} Active" else ""

        if (attendee.signalStrength == SignalStrength.None ||
            attendee.signalStrength == SignalStrength.Low
        ) {
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
