/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AttendeeInfo
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.MeetingService
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.data.RosterAttendee
import com.amazonaws.services.chime.sdkdemo.fragment.DeviceManagementFragment
import com.amazonaws.services.chime.sdkdemo.fragment.MeetingFragment
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

class MeetingActivity : AppCompatActivity(),
    DeviceManagementFragment.DeviceManagementEventListener,
    MeetingFragment.RosterViewEventListener {

    private val logger = ConsoleLogger(LogLevel.DEBUG)

    private lateinit var meetingId: String
    private lateinit var name: String
    private lateinit var mService: MeetingService
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeetingService.MeetingBinder
            mService = binder.getService()
            mBound = true
            mService.audioVideo?.let {
                mService.audioVideo?.startRemoteVideo()

                val rosterViewFragment = MeetingFragment.newInstance(meetingId)
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
                    .commit()
            }
        }
    }
    private val TAG = "InMeetingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)
        meetingId = intent.getStringExtra(HomeActivity.MEETING_ID_KEY) as String
        name = intent.getStringExtra(HomeActivity.NAME_KEY) as String
    }

    override fun onJoinMeetingClicked() {
        val rosterViewFragment = MeetingFragment.newInstance(meetingId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, rosterViewFragment, "rosterViewFragment")
            .commit()
    }

    override fun onLeaveMeeting() {
        onBackPressed()
    }

    override fun onAttendeeAdded(attendeeInfo: Array<AttendeeInfo>) {
        if (mBound) {
            mService.onAttendeesJoined(attendeeInfo)
        }
    }

    override fun onAttendeeRemoved(attendeeInfo: Array<AttendeeInfo>) {
        if (mBound) {
            mService.onAttendeesRemoved(attendeeInfo)
        }
    }

    private fun stopAudioVideoAndUnbindService() {
        mService.audioVideo?.stopRemoteVideo()
        mService.audioVideo?.stopLocalVideo()
        unbindService(connection)
    }

    override fun onBackPressed() {
        if (mBound) {
            stopAudioVideoAndUnbindService()
            MeetingService.stopService(this)
            mBound = false
        }
        super.onBackPressed()
    }

    fun getAudioVideo(): AudioVideoFacade {
        mService.audioVideo
            ?: throw IllegalStateException("AudioVideo should have been initialized")
        return mService.audioVideo as AudioVideoFacade
    }

    fun getMeetingSessionCredentials(): MeetingSessionCredentials {
        mService.credentials
            ?: throw IllegalStateException("Credentials should have been initialized")
        return mService.credentials as MeetingSessionCredentials
    }

    fun getMeetingAttendeeList(): ConcurrentHashMap<String, RosterAttendee> = mService.attendees

    override fun onStop() {
        super.onStop()
        if (mBound) {
            stopAudioVideoAndUnbindService()
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MeetingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        if (mBound) {
            mService.audioVideo?.startRemoteVideo()
        }
    }
}
