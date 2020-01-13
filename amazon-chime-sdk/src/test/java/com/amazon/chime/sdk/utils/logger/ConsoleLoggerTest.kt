package com.amazon.chime.sdk.utils.logger

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConsoleLoggerTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `can be constructed`() {
        val logger = ConsoleLogger()
        assertEquals(LogLevel.WARN, logger.getLogLevel())
    }

    @Test
    fun `can be constructed with different level`() {
        val logger = ConsoleLogger(LogLevel.VERBOSE)
        assertEquals(LogLevel.VERBOSE, logger.getLogLevel())
    }

    @Test
    fun `should log nothing with LogLevel OFF`() {
        val logger = ConsoleLogger(LogLevel.OFF)
        logger.info("tag", "info")
        logger.error("tag", "error")
        verify(exactly = 0) { Log.i(any(), any()) }
        verify(exactly = 0) { Log.e(any(), any()) }
    }

    @Test
    fun `should skip info debug and verbose logs by default`() {
        val logger = ConsoleLogger()
        logger.verbose("tag", "verbose")
        logger.debug("tag", "debug")
        logger.info("tag", "info")
        logger.warn("tag", "warn")
        logger.error("tag", "error")
        verify(exactly = 0) { Log.v(any(), any()) }
        verify(exactly = 0) { Log.d(any(), any()) }
        verify(exactly = 0) { Log.i(any(), any()) }
        verify(exactly = 1) { Log.w("tag", "warn") }
        verify(exactly = 1) { Log.e("tag", "error") }
    }

    @Test
    fun `should have debug and info logs after setting DEBUG log level`() {
        val logger = ConsoleLogger()
        logger.debug("tag", "debug")
        verify(exactly = 0) { Log.d(any(), any()) }
        assertEquals(LogLevel.WARN, logger.getLogLevel())
        logger.setLogLevel(LogLevel.DEBUG)
        logger.debug("tag", "debug")
        logger.info("tag", "info")
        verify(exactly = 1) { Log.d("tag", "debug") }
        verify(exactly = 1) { Log.i("tag", "info") }
    }
}
