package com.amazonaws.services.chime.sdk.meetings.ingestion

import android.content.Context
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DefaultMeetingEventReporterFactoryTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var ingestionConfiguration: IngestionConfiguration

    @MockK
    private lateinit var logger: Logger

    private lateinit var factory: DefaultMeetingEventReporterFactory

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { ingestionConfiguration.ingestionUrl } returns "http://test.com/"
        every { ingestionConfiguration.flushIntervalMs } returns 1L

        factory = DefaultMeetingEventReporterFactory(context, ingestionConfiguration, logger)
    }

    @Test
    fun `createEventReporter should return EventReporter`() {

        every { ingestionConfiguration.disabled } returns false

        val reporter = factory.createEventReporter()

        assertNotNull(reporter)
    }
}
