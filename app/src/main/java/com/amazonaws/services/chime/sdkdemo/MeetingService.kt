/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.CreateAttendeeResponse
import com.amazonaws.services.chime.sdk.meetings.session.CreateMeetingResponse
import com.amazonaws.services.chime.sdk.meetings.session.DefaultMeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity.Companion.MEETING_ID_KEY
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity.Companion.MEETING_RESPONSE_KEY
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity.Companion.NAME_KEY
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.data.JoinMeetingResponse
import com.google.gson.Gson

class MeetingService : Service() {
    private val CHANNEL_ID = "FS"
    private val gson = Gson()
    private val TAG = "MeetingService"
    private val logger = ConsoleLogger()
    var audioVideo: AudioVideoFacade? = null
    var credentials: MeetingSessionCredentials? = null

    private val binder = MeetingBinder()

    inner class MeetingBinder : Binder() {
        fun getService(): MeetingService {
            return this@MeetingService
        }
    }

    companion object {
        fun startService(
            context: Context,
            meetingResponse: String?,
            meetingId: String?,
            attendeeName: String?
        ) {
            val startIntent = Intent(context, MeetingService::class.java)
            startIntent.putExtra(MEETING_RESPONSE_KEY, meetingResponse)
            startIntent.putExtra(MEETING_ID_KEY, meetingId)
            startIntent.putExtra(NAME_KEY, attendeeName)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, MeetingService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // do heavy work on a background thread
        val meetingResponse = intent?.getStringExtra(MEETING_RESPONSE_KEY)
        val meetingId = intent?.getStringExtra(MEETING_ID_KEY)
        val attendeeName = intent?.getStringExtra(NAME_KEY)
        val sessionConfig = createSessionConfiguration(meetingResponse)
        val meetingSession = sessionConfig?.let {
            logger.info(TAG, "Creating meeting session for meeting Id: $meetingId")
            DefaultMeetingSession(
                it,
                logger,
                applicationContext
            )
        }
        credentials = meetingSession?.configuration?.credentials
        audioVideo = meetingSession?.audioVideo
        audioVideo?.start()
        audioVideo?.startRemoteVideo()
        val mediaDevice = audioVideo?.listAudioDevices()
        mediaDevice?.let {
            audioVideo?.chooseAudioDevice(it[1])
        }

        createNotificationChannel()
        val notificationIntent = Intent(this, MeetingActivity::class.java)
        notificationIntent.putExtra(MEETING_RESPONSE_KEY, meetingResponse)
        notificationIntent.putExtra(MEETING_ID_KEY, meetingId)
        notificationIntent.putExtra(NAME_KEY, attendeeName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service Kotlin Example")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioVideo?.stop()
        stopForeground(true)
    }

    private fun createSessionConfiguration(response: String?): MeetingSessionConfiguration? {
        if (response.isNullOrBlank()) return null

        return try {
            val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
            MeetingSessionConfiguration(
                CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
                CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
                ::urlRewriter
            )
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Error creating session configuration: ${exception.localizedMessage}"
            )
            null
        }
    }

    private fun urlRewriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }
}
