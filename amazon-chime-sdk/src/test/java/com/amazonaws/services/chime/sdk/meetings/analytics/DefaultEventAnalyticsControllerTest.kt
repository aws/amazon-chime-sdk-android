/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.analytics

import com.amazonaws.services.chime.sdk.meetings.ingestion.AppState
import com.amazonaws.services.chime.sdk.meetings.ingestion.AppStateMonitor
import com.amazonaws.services.chime.sdk.meetings.ingestion.BatteryState
import com.amazonaws.services.chime.sdk.meetings.ingestion.EventReporter
import com.amazonaws.services.chime.sdk.meetings.ingestion.NetworkConnectionType
import com.amazonaws.services.chime.sdk.meetings.internal.ingestion.SDKEvent
import com.amazonaws.services.chime.sdk.meetings.internal.utils.DeviceUtils
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DefaultEventAnalyticsControllerTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var testEventAnalyticsController: DefaultEventAnalyticsController

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockMeetingSessionConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var eventReporter: EventReporter

    @MockK
    private lateinit var mockMeetingStatsCollector: MeetingStatsCollector

    @MockK
    private lateinit var mockAppStateMonitor: AppStateMonitor

    private val meetingAttributes: EventAttributes = mutableMapOf()
    private val mockMeetingDurationMs = 1000L
    private val mockMeetingReconnectDurationMs = 500L

    @Before
    fun setup() {
        mockkStatic(Calendar::class)

        meetingAttributes[EventAttributeName.meetingDurationMs] = mockMeetingDurationMs
        meetingAttributes[EventAttributeName.meetingReconnectDurationMs] = mockMeetingReconnectDurationMs

        MockKAnnotations.init(this, relaxUnitFun = true)
        testEventAnalyticsController =
            DefaultEventAnalyticsController(
                mockLogger,
                mockMeetingSessionConfiguration,
                mockMeetingStatsCollector,
                mockAppStateMonitor,
                eventReporter
            )
        every { mockMeetingStatsCollector.getMeetingStatsEventAttributes() } returns meetingAttributes
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
        every { mockAppStateMonitor.appState } returns AppState.ACTIVE
        every { mockAppStateMonitor.getBatteryLevel() } returns 0.75f
        every { mockAppStateMonitor.getBatteryState() } returns BatteryState.CHARGING
        every { mockAppStateMonitor.isBatterySaverOn() } returns true

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
    fun `publishEvent should invoked EventReporter's report`() {
        testEventAnalyticsController.publishEvent(EventName.meetingFailed)

        every { eventReporter.report(any()) }

        verify(exactly = 1) { eventReporter.report(any()) }
    }

    @Test
    fun `publishEvent should remove meetingReconnectedDurationMs if not meetingReconnected event`() {
        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs
        testEventAnalyticsController.publishEvent(EventName.meetingFailed)

        verify(exactly = 1) { eventReporter.report(any()) }
        assertNotNull(slot.captured.eventAttributes[EventAttributeName.meetingDurationMs.name])
        assertNull(slot.captured.eventAttributes[EventAttributeName.meetingReconnectDurationMs.name])
    }

    @Test
    fun `publishEvent should contain meetingReconnectedDurationMs if meetingReconnected event`() {
        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.publishEvent(EventName.meetingReconnected)

        verify(exactly = 1) { eventReporter.report(any()) }
        assertNotNull(slot.captured.eventAttributes[EventAttributeName.meetingDurationMs.name])
        assertNotNull(slot.captured.eventAttributes[EventAttributeName.meetingReconnectDurationMs.name])
    }

    @Test
    fun `pushHistory should invoked EventReporter's report`() {
        testEventAnalyticsController.pushHistory(MeetingHistoryEventName.meetingReconnected)

        verify(exactly = 1) { eventReporter.report(any()) }
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

    @Test
    fun `publishEvent should include appState, batteryLevel, and batteryState in event attributes`() {
        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.publishEvent(EventName.meetingFailed)

        verify(exactly = 1) { eventReporter.report(any()) }

        val eventAttributes = slot.captured.eventAttributes
        assertEquals("Active", eventAttributes[EventAttributeName.appState.name])
        assertEquals(0.75f, eventAttributes[EventAttributeName.batteryLevel.name])
        assertEquals("Charging", eventAttributes[EventAttributeName.batteryState.name])
        assertEquals(true.toString(), eventAttributes[EventAttributeName.lowPowerModeEnabled.name])
    }

    @Test
    fun `publishEvent should include appState and batteryState but not batteryLevel when battery level is null`() {
        // Mock battery level as null
        every { mockAppStateMonitor.getBatteryLevel() } returns null

        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.publishEvent(EventName.meetingFailed)

        verify(exactly = 1) { eventReporter.report(any()) }

        val eventAttributes = slot.captured.eventAttributes
        assertEquals("Active", eventAttributes[EventAttributeName.appState.name])
        assertFalse(eventAttributes.containsKey(EventAttributeName.batteryLevel.name))
        assertEquals("Charging", eventAttributes[EventAttributeName.batteryState.name])
        assertEquals(true.toString(), eventAttributes[EventAttributeName.lowPowerModeEnabled.name])
    }

    @Test
    fun `pushHistory should include appState, batteryLevel, and batteryState in event attributes`() {
        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.pushHistory(MeetingHistoryEventName.meetingReconnected)

        verify(exactly = 1) { eventReporter.report(any()) }

        val eventAttributes = slot.captured.eventAttributes
        assertEquals("Active", eventAttributes[EventAttributeName.appState.name])
        assertEquals(0.75f, eventAttributes[EventAttributeName.batteryLevel.name])
        assertEquals("Charging", eventAttributes[EventAttributeName.batteryState.name])
        assertEquals(true.toString(), eventAttributes[EventAttributeName.lowPowerModeEnabled.name])
    }

    @Test
    fun `pushHistory should include appState, batteryState, and lowPowerModeEnabled but not batteryLevel when battery level is null`() {
        // Mock battery level as null
        every { mockAppStateMonitor.getBatteryLevel() } returns null

        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.pushHistory(MeetingHistoryEventName.meetingReconnected)

        verify(exactly = 1) { eventReporter.report(any()) }

        val eventAttributes = slot.captured.eventAttributes
        assertEquals("Active", eventAttributes[EventAttributeName.appState.name])
        assertFalse(eventAttributes.containsKey(EventAttributeName.batteryLevel.name))
        assertEquals("Charging", eventAttributes[EventAttributeName.batteryState.name])
        assertEquals(true.toString(), eventAttributes[EventAttributeName.lowPowerModeEnabled.name])
    }

    @Test
    fun `onNetworkConnectionTypeChanged should publish networkConnectionTypeChanged event`() {

        val slot = slot<SDKEvent>()
        every { eventReporter.report(capture(slot)) } just Runs

        testEventAnalyticsController.onNetworkConnectionTypeChanged(NetworkConnectionType.WIFI)

        verify(exactly = 1) { eventReporter.report(any()) }

        val capturedEvent = slot.captured
        val capturedAttributes = capturedEvent.eventAttributes

        assertEquals(capturedEvent.name, EventName.networkConnectionTypeChanged.name)
        assertEquals(NetworkConnectionType.WIFI.description, capturedAttributes[EventAttributeName.networkConnectionType.name])
    }
}
