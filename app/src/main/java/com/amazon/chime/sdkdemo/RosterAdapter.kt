/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdkdemo

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdkdemo.data.RosterAttendee
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

        when (attendee.volumeLevel) {
            VolumeLevel.Muted -> {
                view.attendeeVolume.setImageResource(R.drawable.volume_muted)
                view.attendeeVolume.contentDescription = "$attendeeName Muted"
            }
            VolumeLevel.NotSpeaking -> {
                view.attendeeVolume.setImageResource(R.drawable.volume_0)
                view.attendeeVolume.contentDescription = "$attendeeName Not Speaking"
            }
            else -> {
                view.attendeeVolume.setImageResource(R.drawable.volume_3)
                view.attendeeVolume.contentDescription = "$attendeeName Speaking"
            }
        }
    }
}
