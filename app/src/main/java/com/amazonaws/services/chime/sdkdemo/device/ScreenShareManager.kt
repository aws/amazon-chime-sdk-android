/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.device

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.os.Build
import com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare.ContentShareSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CaptureSourceObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultScreenCaptureSource
import com.amazonaws.services.chime.sdkdemo.service.ScreenCaptureService

/**
 * [ScreenShareManager] is a wrapper of [ContentShareSource]. It manages
 * [DefaultScreenCaptureSource] and foreground service which required for [MediaProjection]
 * starting [Build.VERSION_CODES.Q].
 */
class ScreenShareManager(
    private val screenCaptureSource: DefaultScreenCaptureSource,
    private val context: Context
) : ContentShareSource() {
    override var videoSource: VideoSource? = screenCaptureSource

    var screenCaptureConnectionService: ServiceConnection? = null

    fun start() = screenCaptureSource.start()

    fun stop(isBound: Boolean = false) {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
        screenCaptureSource.stop()
        screenCaptureConnectionService?.let {
            if (isBound) context.unbindService(it)
        }
    }

    fun release() = screenCaptureSource.release()

    fun addObserver(observer: CaptureSourceObserver) = screenCaptureSource.addCaptureSourceObserver(observer)

    fun removeObserver(observer: CaptureSourceObserver) = screenCaptureSource.removeCaptureSourceObserver(observer)
}
