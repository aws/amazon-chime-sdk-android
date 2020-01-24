package com.amazon.chime.sdkdemo

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazon.chime.sdkdemo.data.RosterAttendee
import kotlinx.android.synthetic.main.roster_view_attendee_row.view.attendeeName

class RosterAdapter(private val roster: MutableCollection<RosterAttendee>) : RecyclerView.Adapter<RosterHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RosterHolder {
        val inflatedView = parent.inflate(R.layout.roster_view_attendee_row, false)
        return RosterHolder(inflatedView)
    }

    override fun getItemCount(): Int {
        return roster.size
    }

    override fun onBindViewHolder(holder: RosterHolder, position: Int) {
        val attendee = roster.elementAt(position)
        holder.bindAttendeeName(attendee)
    }
}

class RosterHolder(inflatedView: View) : RecyclerView.ViewHolder(inflatedView), View.OnClickListener {
    private var view: View = inflatedView
    private var attendeeName: String? = null

    init {
        inflatedView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
    }

    fun bindAttendeeName(attendee: RosterAttendee) {
        this.attendeeName = attendee.attendeeName
        view.attendeeName.text = attendeeName
    }
}
