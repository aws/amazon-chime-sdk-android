/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.session

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.media.AudioManager
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
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
    private lateinit var assetManager: AssetManager

    lateinit var meetingSession: DefaultMeetingSession

    @Before
    fun setup() {
        // Mock Log.d first because initializing the AudioClient mock appears to fail otherwise
        mockkStatic(System::class, Log::class)
        every { System.loadLibrary(any()) } just runs
        every { Log.d(any(), any()) } returns 0
        MockKAnnotations.init(this)
        every { context.assets } returns assetManager
        every { context.registerReceiver(any(), any()) } returns mockkClass(Intent::class)
        every { context.getSystemService(any()) } returns mockkClass(AudioManager::class)
        mockkObject(AudioClientFactory.Companion)
        every { AudioClientFactory.getAudioClient(any(), any()) } returns mockAudioClient
        every { configuration.meetingId } returns "meetingId"
        every { configuration.urls.signalingURL } returns "signalingUrl"
        every { configuration.urls.turnControlURL } returns "turnControlUrl"
        every { configuration.credentials.joinToken } returns "joinToken"

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
