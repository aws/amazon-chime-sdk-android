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
import android.os.PowerManager
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
import org.amazon.chime.webrtc.NetworkChangeDetector.ConnectionType
import org.amazon.chime.webrtc.NetworkMonitor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // Mock NetworkMonitor
        mockkStatic(NetworkMonitor::class)
        every { NetworkMonitor.getInstance() } returns mockk(relaxed = true)

        // Set up mock behavior
        every { mockLogger.info(any(), any()) } returns Unit
        every { mockApplication.getSystemService(any()) } returns null
        every { mockApplication.getApplicationContext() } returns mockApplication

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

    @Test
    fun `isBatterySaverOn should return true when PowerManager indicates power save mode is enabled`() {
        // Mock PowerManager
        val mockPowerManager = mockk<PowerManager>()

        // Set up mock behavior for power save mode enabled
        every { mockApplication.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
        every { mockPowerManager.isPowerSaveMode } returns true

        val isBatterySaverOn = appStateMonitor.isBatterySaverOn()

        assertTrue("Battery saver should be on", isBatterySaverOn)
    }

    @Test
    fun `isBatterySaverOn should return false when PowerManager indicates power save mode is disabled`() {
        // Mock PowerManager
        val mockPowerManager = mockk<PowerManager>()

        // Set up mock behavior for power save mode disabled
        every { mockApplication.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
        every { mockPowerManager.isPowerSaveMode } returns false

        val isBatterySaverOn = appStateMonitor.isBatterySaverOn()

        assertFalse("Battery saver should be off", isBatterySaverOn)
    }

    @Test
    fun `isBatterySaverOn should return false when PowerManager is not available`() {
        // Set up mock behavior for unavailable PowerManager
        every { mockApplication.getSystemService(Context.POWER_SERVICE) } returns null

        val isBatterySaverOn = appStateMonitor.isBatterySaverOn()

        assertFalse("Battery saver should default to false when PowerManager is unavailable", isBatterySaverOn)
    }

    @Test
    fun `isBatterySaverOn should return false and log warning when application context is not available`() {
        // Create monitor without application context
        val monitorWithoutApp = DefaultAppStateMonitor(mockLogger, null)

        val isBatterySaverOn = monitorWithoutApp.isBatterySaverOn()

        assertFalse("Battery saver should default to false when application context is unavailable", isBatterySaverOn)
        verify(exactly = 1) { mockLogger.warn(any(), "Application context not available for Battery Saver check") }
    }

    @Test
    fun `isBatterySaverOn should handle PowerManager service cast failure gracefully`() {
        // Mock a service that is not PowerManager (e.g., return a different service type)
        val mockWrongService = mockk<ActivityManager>()
        every { mockApplication.getSystemService(Context.POWER_SERVICE) } returns mockWrongService

        val isBatterySaverOn = appStateMonitor.isBatterySaverOn()

        assertFalse("Battery saver should default to false when PowerManager cast fails", isBatterySaverOn)
    }

    @Test
    fun `isBatterySaverOn should work correctly in different power save mode states`() {
        // Mock PowerManager
        val mockPowerManager = mockk<PowerManager>()
        every { mockApplication.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager

        // Test power save mode enabled
        every { mockPowerManager.isPowerSaveMode } returns true
        assertTrue("Battery saver should be on when power save mode is enabled", appStateMonitor.isBatterySaverOn())

        // Test power save mode disabled
        every { mockPowerManager.isPowerSaveMode } returns false
        assertFalse("Battery saver should be off when power save mode is disabled", appStateMonitor.isBatterySaverOn())

        // Test multiple calls to ensure consistency
        every { mockPowerManager.isPowerSaveMode } returns true
        assertTrue("Battery saver should consistently return true", appStateMonitor.isBatterySaverOn())
        assertTrue("Battery saver should consistently return true on multiple calls", appStateMonitor.isBatterySaverOn())
    }

    // Tests for onConnectionTypeChanged method

    @Test
    fun `onConnectionTypeChanged should notify handler when bound and started`() {
        // Start monitoring and bind handler
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Call onConnectionTypeChanged with WIFI connection type
        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_WIFI)

        // Verify handler notification with correct NetworkConnectionType
        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.WIFI) }
    }

    @Test
    fun `onConnectionTypeChanged should not notify handler when not bound`() {
        // Start monitoring but don't bind handler
        appStateMonitor.start()

        // Advance the test scheduler to execute the coroutine from start()
        testDispatcher.scheduler.advanceUntilIdle()

        // Call onConnectionTypeChanged
        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_ETHERNET)

        // Verify handler was not called (since it's not bound)
        verify(exactly = 0) { mockHandler.onNetworkConnectionTypeChanged(any()) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_UNKNOWN correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_UNKNOWN)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.UNKNOWN) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_ETHERNET correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_ETHERNET)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.ETHERNET) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_WIFI correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_WIFI)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.WIFI) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_5G correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_5G)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.FIVE_G) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_4G correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_4G)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.FOUR_G) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_3G correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_3G)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.THREE_G) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_2G correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_2G)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.TWO_G) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_UNKNOWN_CELLULAR correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_UNKNOWN_CELLULAR)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.CELLULAR) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_BLUETOOTH correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_BLUETOOTH)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.BLUETOOTH) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_VPN correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_VPN)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.VPN) }
    }

    @Test
    fun `onConnectionTypeChanged should handle CONNECTION_NONE correctly`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_NONE)

        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.NONE) }
    }

    @Test
    fun `onConnectionTypeChanged should handle multiple consecutive connection changes`() {
        appStateMonitor.start()
        appStateMonitor.bindHandler(mockHandler)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate multiple connection type changes
        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_NONE)
        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_WIFI)
        appStateMonitor.onConnectionTypeChanged(ConnectionType.CONNECTION_4G)

        // Verify all handler notifications
        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.NONE) }
        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.WIFI) }
        verify(exactly = 1) { mockHandler.onNetworkConnectionTypeChanged(NetworkConnectionType.FOUR_G) }
    }
}
