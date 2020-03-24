/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.utils.logger

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConsoleLoggerTest {
    private val testTag = "tag"
    private val testInfoMsg = "info"
    private val testDebugMsg = "debug"
    private val testWarnMsg = "warn"
    private val testErrorMsg = "error"
    private val testVerboseMsg = "verbose"

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
    fun `constructor should use default log level when no parameters`() {
        val logger = ConsoleLogger()

        assertEquals(LogLevel.WARN, logger.getLogLevel())
    }

    @Test
    fun `constructor should use the log level when given parameter`() {
        val logger = ConsoleLogger(LogLevel.VERBOSE)

        assertEquals(LogLevel.VERBOSE, logger.getLogLevel())
    }

    @Test
    fun `logger should log nothing when log level is OFF`() {
        val logger = ConsoleLogger(LogLevel.OFF)

        logger.info(testTag, testInfoMsg)
        logger.error(testTag, testErrorMsg)

        verify(exactly = 0) { Log.i(any(), any()) }
        verify(exactly = 0) { Log.e(any(), any()) }
    }

    @Test
    fun `logger should skip messages of log levels lower than configured log level`() {
        val logger = ConsoleLogger()

        logger.verbose(testTag, testVerboseMsg)
        logger.debug(testTag, testDebugMsg)
        logger.info(testTag, testInfoMsg)
        logger.warn(testTag, testWarnMsg)
        logger.error(testTag, testErrorMsg)

        verify(exactly = 0) { Log.v(any(), any()) }
        verify(exactly = 0) { Log.d(any(), any()) }
        verify(exactly = 0) { Log.i(any(), any()) }
        verify(exactly = 1) { Log.w(testTag, testWarnMsg) }
        verify(exactly = 1) { Log.e(testTag, testErrorMsg) }
    }

    @Test
    fun `logger should log messages of all log levels greater than or equal to configured log level`() {
        val logger = ConsoleLogger()
        logger.setLogLevel(LogLevel.VERBOSE)

        logger.verbose(testTag, testVerboseMsg)
        logger.debug(testTag, testDebugMsg)
        logger.info(testTag, testInfoMsg)
        logger.warn(testTag, testWarnMsg)
        logger.error(testTag, testErrorMsg)

        verify(exactly = 1) { Log.v(testTag, testVerboseMsg) }
        verify(exactly = 1) { Log.d(testTag, testDebugMsg) }
        verify(exactly = 1) { Log.i(testTag, testInfoMsg) }
        verify(exactly = 1) { Log.w(testTag, testWarnMsg) }
        verify(exactly = 1) { Log.e(testTag, testErrorMsg) }
    }
}
