package com.amazon.chime.sdkdemo

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.amazon.chime.sdkdemo.data.RosterAttendee
import kotlinx.android.synthetic.main.roster_view_attendee_row.view.attendeeName
import kotlinx.android.synthetic.main.roster_view_attendee_row.view.attendeeVolume

class RosterAdapter(
    private val roster: MutableCollection<RosterAttendee>,
    private val context: Context
) :
    RecyclerView.Adapter<RosterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RosterHolder {
        val inflatedView = parent.inflate(R.layout.roster_view_attendee_row, false)
        return RosterHolder(inflatedView, context)
    }

    override fun getItemCount(): Int {
        return roster.size
    }

    override fun onBindViewHolder(holder: RosterHolder, position: Int) {
        val attendee = roster.elementAt(position)
        holder.bindAttendee(attendee)
    }
}

class RosterHolder(inflatedView: View, private val context: Context) :
    RecyclerView.ViewHolder(inflatedView) {
    private val MUTED = -1
    private val NOT_SPEAKING = 0

    private var view: View = inflatedView
    private var attendeeName: String? = null

    fun bindAttendee(attendee: RosterAttendee) {
        this.attendeeName = attendee.attendeeName
        view.attendeeName.text = attendeeName

        when (attendee.score) {
            MUTED -> {
                view.attendeeVolume.text = context.getString(R.string.volume_muted)
                view.attendeeVolume.setBackgroundColor(getColor(context, R.color.colorMuted))
            }
            NOT_SPEAKING -> {
                view.attendeeVolume.text = ""
                view.attendeeVolume.setBackgroundColor(
                    getColor(
                        context,
                        android.R.color.transparent
                    )
                )
            }
            else -> {
                view.attendeeVolume.text = context.getString(R.string.volume_speaking)
                view.attendeeVolume.setBackgroundColor(getColor(context, R.color.colorSpeaking))
            }
        }
    }
}
