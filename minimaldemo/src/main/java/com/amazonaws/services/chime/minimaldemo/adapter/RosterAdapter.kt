/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.minimaldemo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.minimaldemo.R
import com.amazonaws.services.chime.minimaldemo.data.RosterAttendee
import com.amazonaws.services.chime.sdk.meetings.audiovideo.SignalStrength
import com.amazonaws.services.chime.sdk.meetings.audiovideo.VolumeLevel
import com.amazonaws.services.chime.sdk.meetings.internal.AttendeeStatus

class RosterAdapter(
    private val roster: Collection<RosterAttendee>
) :
    RecyclerView.Adapter<RosterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RosterHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.row_roster, parent, false)
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

    private val attendeeName: TextView = inflatedView.findViewById(R.id.attendeeName)
    private val activeSpeakerIndicator: ImageView = inflatedView.findViewById(R.id.activeSpeakerIndicator)
    private val attendeeVolume: ImageView = inflatedView.findViewById(R.id.attendeeVolume)

    fun bindAttendee(attendee: RosterAttendee) {
        attendeeName.text = attendee.attendeeName
        attendeeName.contentDescription = attendee.attendeeName
        activeSpeakerIndicator.visibility = if (attendee.isActiveSpeaker) View.VISIBLE else View.INVISIBLE
        activeSpeakerIndicator.contentDescription = if (attendee.isActiveSpeaker) "${attendee.attendeeName} Active" else ""

        if (attendee.attendeeStatus == AttendeeStatus.Joined) {
            if (attendee.signalStrength == SignalStrength.None ||
                attendee.signalStrength == SignalStrength.Low
            ) {
                val drawable = if (attendee.volumeLevel == VolumeLevel.Muted) {
                    R.drawable.ic_microphone_poor_connectivity_dissabled
                } else {
                    R.drawable.ic_microphone_poor_connectivity
                }
                attendeeVolume.setImageResource(drawable)
                view.contentDescription = "$attendeeName Signal Strength Poor"
            } else {
                when (attendee.volumeLevel) {
                    VolumeLevel.Muted -> {
                        attendeeVolume.setImageResource(R.drawable.ic_microphone_disabled)
                        attendeeVolume.contentDescription = "$attendeeName Muted"
                    }
                    VolumeLevel.NotSpeaking -> {
                        attendeeVolume.setImageResource(R.drawable.ic_microphone_enabled)
                        attendeeVolume.contentDescription = "$attendeeName Not Speaking"
                    }
                    VolumeLevel.Low -> {
                        attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_1)
                        attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                    VolumeLevel.Medium -> {
                        attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_2)
                        attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                    VolumeLevel.High -> {
                        attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_3)
                        attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                }
            }
            attendeeVolume.visibility = View.VISIBLE
        } else {
            attendeeVolume.visibility = View.GONE
        }
    }
}
