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
import com.amazonaws.services.chime.sdk.meetings.internal.AttendeeStatus
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.databinding.RowRosterBinding
import com.amazonaws.services.chime.sdkdemo.utils.inflate
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

    private val binding = RowRosterBinding.bind(inflatedView)

    fun bindAttendee(attendee: RosterAttendee) {
        val attendeeName = attendee.attendeeName
        binding.attendeeName.text = attendeeName
        binding.attendeeName.contentDescription = attendeeName
        binding.activeSpeakerIndicator.visibility = if (attendee.isActiveSpeaker) View.VISIBLE else View.INVISIBLE
        binding.activeSpeakerIndicator.contentDescription = if (attendee.isActiveSpeaker) "${attendee.attendeeName} Active" else ""

        if (attendee.attendeeStatus == AttendeeStatus.Joined) {
            if (attendee.signalStrength == SignalStrength.None ||
                    attendee.signalStrength == SignalStrength.Low
            ) {
                val drawable = if (attendee.volumeLevel == VolumeLevel.Muted) {
                    R.drawable.ic_microphone_poor_connectivity_dissabled
                } else {
                    R.drawable.ic_microphone_poor_connectivity
                }
                binding.attendeeVolume.setImageResource(drawable)
                binding.attendeeVolume.contentDescription = "$attendeeName Signal Strength Poor"
            } else {
                when (attendee.volumeLevel) {
                    VolumeLevel.Muted -> {
                        binding.attendeeVolume.setImageResource(R.drawable.ic_microphone_disabled)
                        binding.attendeeVolume.contentDescription = "$attendeeName Muted"
                    }
                    VolumeLevel.NotSpeaking -> {
                        binding.attendeeVolume.setImageResource(R.drawable.ic_microphone_enabled)
                        binding.attendeeVolume.contentDescription = "$attendeeName Not Speaking"
                    }
                    VolumeLevel.Low -> {
                        binding.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_1)
                        binding.attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                    VolumeLevel.Medium -> {
                        binding.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_2)
                        binding.attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                    VolumeLevel.High -> {
                        binding.attendeeVolume.setImageResource(R.drawable.ic_microphone_audio_3)
                        binding.attendeeVolume.contentDescription = "$attendeeName Speaking"
                    }
                }
            }
            binding.attendeeVolume.visibility = View.VISIBLE
        } else {
            binding.attendeeVolume.visibility = View.GONE
        }
    }
}
