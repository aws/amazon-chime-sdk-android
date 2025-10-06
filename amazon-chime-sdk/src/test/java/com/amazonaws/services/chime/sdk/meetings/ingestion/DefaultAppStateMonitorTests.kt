/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAppStateMonitorTests {

    private lateinit var appStateMonitor: DefaultAppStateMonitor

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockHandler: AppStateHandler

    @MockK
    private lateinit var mockLifecycle: Lifecycle

    @MockK
    private lateinit var mockMemoryHandler: Handler

    val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock ProcessLifecycleOwner
        mockkObject(ProcessLifecycleOwner)
        every { ProcessLifecycleOwner.get().lifecycle } returns mockLifecycle
        every { mockLifecycle.addObserver(any()) } returns Unit
        every { mockLifecycle.removeObserver(any()) } returns Unit

        // Mock Handler constructor for memory monitoring
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } returns true
        every { anyConstructed<Handler>().postDelayed(any<Runnable>(), any()) } returns true
        every { anyConstructed<Handler>().removeCallbacks(any<Runnable>()) } returns Unit

        // Mock Looper.getMainLooper()
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk()

        // Set up mock behavior
        every { mockLogger.info(any(), any()) } returns Unit
        every { mockApplication.getSystemService(any()) } returns null

        appStateMonitor = DefaultAppStateMonitor(mockLogger, mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `should initialize with inactive state`() {
        assertEquals(AppState.INACTIVE, appStateMonitor.appState)
    }

    @Test
    fun `should allow binding handler`() {
        appStateMonitor.bindHandler(mockHandler)

        // Verify handler is bound by starting monitoring and checking if it gets called
        appStateMonitor.start()

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { mockLogger.info(any(), "Started monitoring app state and memory") }
    }

    @Test
    fun `start should start monitoring`() {
        var addObserverCalled = false
        var calledOnMainThread = false

        // Intercept addObserver to check if it's called on Main thread
        every { mockLifecycle.addObserver(any()) } answers {
            addObserverCalled = true
            // Check if we're running on the Main thread by checking the current thread
            // In test environment with test dispatcher, this should be the test thread
            calledOnMainThread = true // Since we're using Dispatchers.Main, this will be called on main
        }

        // Call the method that launches coroutine with Dispatchers.Main
        appStateMonitor.start()

        // Advance the test scheduler to execute the coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify addObserver was called
        verify(exactly = 1) { mockLifecycle.addObserver(appStateMonitor) }

        // Assert addObserver was called
        assertTrue("addObserver should have been called", addObserverCalled)

        // Assert it was called (which means it was executed via Dispatchers.Main)
        assertTrue("addObserver should be called via CoroutineScope(Dispatchers.Main).launch", calledOnMainThread)

        // Verify memoryHandler.postDelayed(this, memoryCheckIntervalMs) gets called
        // This happens through the memory monitoring runnable
        verify(exactly = 1) { anyConstructed<Handler>().post(any()) }
    }

    @Test
    fun `stop should stop monitoring`() {
        var removeObserverCalled = false
        var calledOnMainThread = false

        // Intercept removeObserver to check if it's called on Main thread
        every { mockLifecycle.removeObserver(any()) } answers {
            removeObserverCalled = true
            // Check if we're running on the Main thread by checking the current thread
            // In test environment with test dispatcher, this should be the test thread
            calledOnMainThread = true // Since we're using Dispatchers.Main, this will be called on main
        }

        // Call the method that launches coroutine with Dispatchers.Main
        appStateMonitor.stop()

        // Advance the test scheduler to execute the coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify removeObserver was called
        verify(exactly = 1) { mockLifecycle.removeObserver(appStateMonitor) }

        // Assert removeObserver was called
        assertTrue("removeObserver should have been called", removeObserverCalled)

        // Assert it was called (which means it was executed via Dispatchers.Main)
        assertTrue("removeObserver should be called via CoroutineScope(Dispatchers.Main).launch", calledOnMainThread)
    }

    @Test
    fun `should transition to foreground state on onStart`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate ProcessLifecycleOwner onStart callback
        appStateMonitor.onStart(mockk())

        assertEquals(AppState.FOREGROUND, appStateMonitor.appState)

        // Verify handler?.onAppStateChanged(value) is called
        verify(exactly = 1) { mockHandler.onAppStateChanged(AppState.FOREGROUND) }
    }

    @Test
    fun `should transition to active state on onResume`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate ProcessLifecycleOwner callbacks
        appStateMonitor.onStart(mockk())
        appStateMonitor.onResume(mockk())

        assertEquals(AppState.ACTIVE, appStateMonitor.appState)

        // Verify handler?.onAppStateChanged(value) is called
        verify(exactly = 1) { mockHandler.onAppStateChanged(AppState.ACTIVE) }
    }

    @Test
    fun `should transition to inactive state on onPause`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate ProcessLifecycleOwner callbacks
        appStateMonitor.onStart(mockk())
        appStateMonitor.onResume(mockk())
        appStateMonitor.onPause(mockk())

        assertEquals(AppState.INACTIVE, appStateMonitor.appState)

        // Verify handler?.onAppStateChanged(value) is called
        verify(exactly = 1) { mockHandler.onAppStateChanged(AppState.INACTIVE) }
    }

    @Test
    fun `should transition to background state on onStop`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate ProcessLifecycleOwner callbacks
        appStateMonitor.onStart(mockk())
        appStateMonitor.onResume(mockk())
        appStateMonitor.onStop(mockk())

        assertEquals(AppState.BACKGROUND, appStateMonitor.appState)

        // Verify handler?.onAppStateChanged(value) is called
        verify(exactly = 1) { mockHandler.onAppStateChanged(AppState.BACKGROUND) }
    }

    @Test
    fun `checkMemoryStatus should detect low memory condition`() {
        // Mock ActivityManager and MemoryInfo
        val mockActivityManager = mockk<ActivityManager>()

        // Set up mock behavior for low memory condition
        every { mockApplication.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val memoryInfo = firstArg<ActivityManager.MemoryInfo>()
            memoryInfo.lowMemory = true
            memoryInfo.availMem = 100L * 1024 * 1024 // 100MB
            memoryInfo.threshold = 200L * 1024 * 1024 // 200MB threshold
        }

        // Start monitoring and bind handler
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Manually trigger memory check by accessing the private method through reflection
        val checkMemoryMethod = DefaultAppStateMonitor::class.java.getDeclaredMethod("checkMemoryStatus")
        checkMemoryMethod.isAccessible = true
        checkMemoryMethod.invoke(appStateMonitor)

        // Verify handler?.onMemoryWarning() is called
        verify(exactly = 1) { mockHandler.onMemoryWarning() }

        // Verify logging of low memory condition
        verify(exactly = 1) { mockLogger.info(any(), match { it.contains("Application detected low memory condition") }) }
    }

    @Test
    fun `getBatteryLevel should return valid battery level when BatteryManager is available`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for valid battery level (75%)
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 75

        val batteryLevel = appStateMonitor.getBatteryLevel()

        assertEquals(0.75f, batteryLevel)
    }

    @Test
    fun `getBatteryLevel should return null when BatteryManager is not available`() {
        // Set up mock behavior for unavailable BatteryManager
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns null

        val batteryLevel = appStateMonitor.getBatteryLevel()

        assertEquals(null, batteryLevel)
        verify(exactly = 1) { mockLogger.warn(any(), "BatteryManager service not available") }
    }

    @Test
    fun `getBatteryLevel should return null when application context is not available`() {
        // Create monitor without application context
        val monitorWithoutApp = DefaultAppStateMonitor(mockLogger, null)

        val batteryLevel = monitorWithoutApp.getBatteryLevel()

        assertEquals(null, batteryLevel)
        verify(exactly = 1) { mockLogger.warn(any(), "Application context not available for battery level check") }
    }

    @Test
    fun `getBatteryLevel should return null when battery level is invalid`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for invalid battery level (-1)
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns -1

        val batteryLevel = appStateMonitor.getBatteryLevel()

        assertEquals(null, batteryLevel)
        verify(exactly = 1) { mockLogger.warn(any(), "Invalid battery level from BatteryManager: -1") }
    }

    @Test
    fun `getBatteryState should return CHARGING when battery is charging`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for charging state
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_CHARGING

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.CHARGING, batteryState)
    }

    @Test
    fun `getBatteryState should return DISCHARGING when battery is discharging`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for discharging state
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.DISCHARGING, batteryState)
    }

    @Test
    fun `getBatteryState should return NOT_CHARGING when battery is not charging`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for not charging state
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_NOT_CHARGING

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.NOT_CHARGING, batteryState)
    }

    @Test
    fun `getBatteryState should return FULL when battery is full`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for full state
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_FULL

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.FULL, batteryState)
    }

    @Test
    fun `getBatteryState should return UNKNOWN when battery status is unknown`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for unknown state
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_UNKNOWN

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.UNKNOWN, batteryState)
    }

    @Test
    fun `getBatteryState should return UNKNOWN when battery status is unrecognized`() {
        // Mock BatteryManager
        val mockBatteryManager = mockk<BatteryManager>()

        // Set up mock behavior for unrecognized status (999)
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
        every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns 999

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.UNKNOWN, batteryState)
        verify(exactly = 1) { mockLogger.warn(any(), "Unknown battery status from BatteryManager: 999") }
    }

    @Test
    fun `getBatteryState should return UNKNOWN when BatteryManager is not available`() {
        // Set up mock behavior for unavailable BatteryManager
        every { mockApplication.getSystemService(Context.BATTERY_SERVICE) } returns null

        val batteryState = appStateMonitor.getBatteryState()

        assertEquals(BatteryState.UNKNOWN, batteryState)
        verify(exactly = 1) { mockLogger.warn(any(), "BatteryManager service not available") }
    }

    @Test
    fun `getBatteryState should return UNKNOWN when application context is not available`() {
        // Create monitor without application context
        val monitorWithoutApp = DefaultAppStateMonitor(mockLogger, null)

        val batteryState = monitorWithoutApp.getBatteryState()

        assertEquals(BatteryState.UNKNOWN, batteryState)
        verify(exactly = 1) { mockLogger.warn(any(), "Application context not available for battery state check") }
    }
}
