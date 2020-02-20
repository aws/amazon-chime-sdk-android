/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.session

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.amazon.chime.sdk.media.clientcontroller.AudioClientSingleton
import com.amazon.chime.sdk.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DefaultMeetingSessionTest {
    @MockK
    lateinit var configuration: MeetingSessionConfiguration

    @MockK
    lateinit var logger: Logger

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var mockAudioClient: AudioClient

    @MockK
    lateinit var mockAudioClientSingleton: AudioClientSingleton

    lateinit var meetingSession: DefaultMeetingSession

    @Before
    fun setup() {
        // Mock Log.d first because initializing the AudioClient mock appears to fail otherwise
        mockkStatic(System::class, Log::class)
        every { System.loadLibrary(any()) } just runs
        every { Log.d(any(), any()) } returns 0
        MockKAnnotations.init(this)
        every { context.registerReceiver(any(), any()) } returns mockkClass(Intent::class)
        every { context.getSystemService(any()) } returns mockkClass(AudioManager::class)
        mockkObject(AudioClientSingleton.Companion)
        every { AudioClientSingleton.getInstance(any()) } returns mockAudioClientSingleton
        every { mockAudioClientSingleton.audioClient } returns mockAudioClient
        meetingSession = DefaultMeetingSession(configuration, logger, context)
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
