/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.internal.utils.DeviceUtils
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultEventAnalyticsControllerTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var testEventAnalyticsController: EventAnalyticsController

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockMeetingSessionConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockMeetingStatsCollector: MeetingStatsCollector

    private val meetingDurationAttribute: EventAttributes =
        mutableMapOf(EventAttributeName.meetingDurationMs to 1000L)

    @Before
    fun setup() {
        mockkStatic(Calendar::class)

        MockKAnnotations.init(this, relaxUnitFun = true)
        testEventAnalyticsController =
            DefaultEventAnalyticsController(
                mockLogger,
                mockMeetingSessionConfiguration,
                mockMeetingStatsCollector
            )
        every { mockMeetingStatsCollector.getMeetingStatsEventAttributes() } returns meetingDurationAttribute
        every { mockMeetingStatsCollector.getMeetingHistory() } returns emptyList()
        every { mockMeetingSessionConfiguration.credentials } returns MeetingSessionCredentials(
            "attendeeId",
            "externalUserId",
            "joinToken"
        )
        every { mockMeetingSessionConfiguration.externalMeetingId } returns "externalMeetingId"
        every { mockMeetingSessionConfiguration.meetingId } returns "meetingId"
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceManufacturer } returns "deviceManufacturer"
        every { DeviceUtils.deviceModel } returns "deviceModel"
        every { DeviceUtils.deviceName } returns "deviceName"
        every { DeviceUtils.sdkVersion } returns "sdkVersion"
        every { DeviceUtils.sdkName } returns "sdkName"
        every { DeviceUtils.osName } returns "osName"
        every { DeviceUtils.osVersion } returns "osVersion"

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `onEventReceived should be invoked when publishEvent is called`() {
        val observer = spyk<EventAnalyticsObserver>()
        testEventAnalyticsController.addEventAnalyticsObserver(observer)
        runBlockingTest {
            testEventAnalyticsController.publishEvent(EventName.meetingFailed)
        }

        verify(exactly = 1) { observer.onEventReceived(withArg {
            assertEquals(EventName.meetingFailed, it)
        }, any()) }
    }

    @Test
    fun `onEventReceived should not be invoked when publishEvent is called when no observer is available `() {
        val observer = spyk<EventAnalyticsObserver>()
        testEventAnalyticsController.addEventAnalyticsObserver(observer)
        testEventAnalyticsController.removeEventAnalyticsObserver(observer)

        runBlockingTest {
            testEventAnalyticsController.publishEvent(EventName.meetingFailed)
        }

        verify(exactly = 0) { observer.onEventReceived(any(), any()) }
    }

    @Test
    fun `onEventReceived should add event when publishEvent is called when no observer is available `() {
        val observer = spyk<EventAnalyticsObserver>()
        testEventAnalyticsController.addEventAnalyticsObserver(observer)
        testEventAnalyticsController.removeEventAnalyticsObserver(observer)

        runBlockingTest {
            testEventAnalyticsController.publishEvent(EventName.meetingFailed)
        }

        verify(exactly = 0) { observer.onEventReceived(any(), any()) }
    }

    @Test
    fun `onEventReceived should be invoked with meeting stats when event is meeting life cycle related`() {
        val observer = spyk<EventAnalyticsObserver>()
        testEventAnalyticsController.addEventAnalyticsObserver(observer)

        runBlockingTest {
            testEventAnalyticsController.publishEvent(EventName.meetingFailed)
        }

        verify(exactly = 1) {
            observer.onEventReceived(
                EventName.meetingFailed,
                withArg { assertTrue(it.containsKey(EventAttributeName.meetingDurationMs)) })
        }
    }

    @Test
    fun `onEventReceived should be invoked without meeting stats when event is not meeting life cycle related`() {
        val observer = spyk<EventAnalyticsObserver>()
        testEventAnalyticsController.addEventAnalyticsObserver(observer)

        runBlockingTest {
            testEventAnalyticsController.publishEvent(EventName.videoInputFailed)
        }

        verify(exactly = 1) { observer.onEventReceived(
            EventName.videoInputFailed,
            withArg { assertFalse(it.containsKey(EventAttributeName.meetingDurationMs)) })
        }
    }

    @Test
    fun `getCommonEventAttributes should return common attributes`() {
        val attributes = testEventAnalyticsController.getCommonEventAttributes()

        assertTrue(attributes.containsKey(EventAttributeName.attendeeId))
        assertTrue(attributes.containsKey(EventAttributeName.deviceName))
        assertTrue(attributes.containsKey(EventAttributeName.deviceManufacturer))
        assertTrue(attributes.containsKey(EventAttributeName.deviceModel))
        assertTrue(attributes.containsKey(EventAttributeName.sdkVersion))
        assertTrue(attributes.containsKey(EventAttributeName.mediaSdkVersion))
        assertTrue(attributes.containsKey(EventAttributeName.meetingId))
    }

    @Test
    fun `getMeetingHistory should call meetingStatsCollector to get history events`() {
        testEventAnalyticsController.getMeetingHistory()

        verify { mockMeetingStatsCollector.getMeetingHistory() }
    }
}
