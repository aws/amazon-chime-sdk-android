package com.amazon.chime.sdk.session

import android.content.Context
import android.media.AudioManager
import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import com.amazon.chime.sdk.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.mockkObject
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
    lateinit var audioClientController: AudioClientController

    lateinit var meetingSession: DefaultMeetingSession

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.getSystemService(any()) } returns mockkClass(AudioManager::class)
        mockkObject(AudioClientController.Companion)
        every { AudioClientController.getInstance(any()) } returns audioClientController
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
