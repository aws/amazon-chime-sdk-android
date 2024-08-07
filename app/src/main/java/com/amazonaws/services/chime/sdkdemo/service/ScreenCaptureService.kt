/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.utils.isOSVersionAtLeast

class ScreenCaptureService : Service() {
    private lateinit var notificationManager: NotificationManager

    private val CHANNEL_ID = "ScreenCaptureServiceChannelID"
    private val CHANNEL_NAME = "Screen Share"
    private val SERVICE_ID = 1

    private val binder = ScreenCaptureBinder()

    inner class ScreenCaptureBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isOSVersionAtLeast(Build.VERSION_CODES.O)) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        if (isOSVersionAtLeast(Build.VERSION_CODES.Q)) {
            startForeground(
                SERVICE_ID,
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.screen_capture_notification_tile))
                    .setContentText(getText(R.string.screen_capture_notification_text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                SERVICE_ID,
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.screen_capture_notification_tile))
                    .setContentText(getText(R.string.screen_capture_notification_text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
            )
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = binder
}
