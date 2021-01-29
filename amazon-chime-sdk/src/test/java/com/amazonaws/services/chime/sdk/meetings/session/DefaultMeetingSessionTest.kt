/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DefaultMeetingSessionTest {
    @MockK
    private lateinit var configuration: MeetingSessionConfiguration

    @MockK
    private lateinit var contentConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var mockAudioClient: AudioClient

    @MockK
    private lateinit var assetManager: AssetManager

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    @MockK
    private lateinit var mockLooper: Looper

    private lateinit var meetingSession: DefaultMeetingSession

    @Before
    fun setup() {
        // Mock Log.d first because initializing the AudioClient mock appears to fail otherwise
        mockkStatic(System::class, Log::class)
        every { System.loadLibrary(any()) } just runs
        every { Log.d(any(), any()) } returns 0
        MockKAnnotations.init(this, relaxed = true)
        every { context.assets } returns assetManager
        every { context.registerReceiver(any(), any()) } returns mockkClass(Intent::class)
        val audioManager = mockkClass(AudioManager::class)
        every { audioManager.mode = any() } returns Unit
        every { audioManager.mode } returns AudioManager.MODE_NORMAL
        every { audioManager.isSpeakerphoneOn } returns true
        val cameraManager = mockkClass(CameraManager::class)
        every { cameraManager.cameraIdList } returns emptyArray()
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        mockkObject(AudioClientFactory.Companion)
        every { AudioClientFactory.getAudioClient(any(), any()) } returns mockAudioClient
        every { logger.info(any(), any()) } just runs
        every { configuration.createContentShareMeetingSessionConfiguration() } returns contentConfiguration

        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockLooper
        every { anyConstructed<HandlerThread>().run() } just runs

        meetingSession = DefaultMeetingSession(configuration, logger, context, mockEglCoreFactory)
    }

    @Test
    fun `constructor should return non-null instance`() {
        assertNotNull(meetingSession)
    }

    @Test
    fun `audioVideo should return non-null instance`() {
        assertNotNull(meetingSession.audioVideo)
    }
}
