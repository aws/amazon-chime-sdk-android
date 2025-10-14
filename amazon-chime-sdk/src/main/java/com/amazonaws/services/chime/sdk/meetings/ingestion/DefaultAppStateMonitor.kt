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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.amazon.chime.webrtc.NetworkChangeDetector.ConnectionType
import org.amazon.chime.webrtc.NetworkMonitor
import org.amazon.chime.webrtc.NetworkMonitor.NetworkObserver

class DefaultAppStateMonitor(
    private val logger: Logger,
    private val application: Application? = null,
    private val memoryCheckIntervalMs: Long = 5000L // Check memory every 5 seconds
) : AppStateMonitor, DefaultLifecycleObserver, NetworkObserver {

    private val TAG = "DefaultAppStateMonitor"

    private var handler: AppStateHandler? = null

    // App states should be posted only when the meeting session is running
    private var shouldPostEvent: Boolean = false

    // Continuous memory monitoring
    private val memoryHandler = Handler(Looper.getMainLooper())
    private var memoryCheckRunnable: Runnable? = null
    private var isMemoryMonitoringActive = false

    private var _appState: AppState = AppState.INACTIVE
        set(value) {
            field = value
            logger.info(TAG, "Application entered state: ${value.description}")
            if (shouldPostEvent) {
                handler?.onAppStateChanged(value)
            }
        }

    override val appState: AppState
        get() = _appState

    init {
        application?.let {
            NetworkMonitor.getInstance().startMonitoring(it.applicationContext, "")
        }
    }

    override fun bindHandler(handler: AppStateHandler) {
        this.handler = handler
    }

    override fun start() {
        CoroutineScope(Dispatchers.Main).launch {
            // Removing existing observers and monitors if there are any
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this@DefaultAppStateMonitor)
            stopMemoryMonitoring()
            NetworkMonitor.getInstance().removeObserver(this@DefaultAppStateMonitor)

            shouldPostEvent = true

            // Register for process lifecycle events
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@DefaultAppStateMonitor)

            // Start continuous memory monitoring
            startMemoryMonitoring()

            NetworkMonitor.getInstance().addObserver(this@DefaultAppStateMonitor)

            logger.info(TAG, "Started monitoring app state and memory")
        }
    }

    override fun stop() {
        CoroutineScope(Dispatchers.Main).launch {
            shouldPostEvent = false

            ProcessLifecycleOwner.get().lifecycle.removeObserver(this@DefaultAppStateMonitor)

            // Stop continuous memory monitoring
            stopMemoryMonitoring()

            NetworkMonitor.getInstance().removeObserver(this@DefaultAppStateMonitor)

            logger.info(TAG, "Stopped monitoring app state and memory")
        }
    }

    // ProcessLifecycleOwner callbacks
    override fun onCreate(owner: LifecycleOwner) {
        // no-op
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // no-op
    }

    override fun onStart(owner: LifecycleOwner) {
        _appState = AppState.FOREGROUND
    }

    override fun onResume(owner: LifecycleOwner) {
        _appState = AppState.ACTIVE
    }

    override fun onPause(owner: LifecycleOwner) {
        _appState = AppState.INACTIVE
    }

    override fun onStop(owner: LifecycleOwner) {
        _appState = AppState.BACKGROUND
    }

    // Continuous memory monitoring methods
    private fun startMemoryMonitoring() {
        if (isMemoryMonitoringActive) return

        isMemoryMonitoringActive = true
        memoryCheckRunnable = object : Runnable {
            override fun run() {
                if (isMemoryMonitoringActive) {
                    checkMemoryStatus()
                    memoryHandler.postDelayed(this, memoryCheckIntervalMs)
                }
            }
        }
        memoryCheckRunnable?.let { memoryHandler.post(it) }
        logger.info(TAG, "Started continuous memory monitoring (interval: ${memoryCheckIntervalMs}ms)")
    }

    private fun stopMemoryMonitoring() {
        if (!isMemoryMonitoringActive) return

        isMemoryMonitoringActive = false
        memoryCheckRunnable?.let { memoryHandler.removeCallbacks(it) }
        memoryCheckRunnable = null
        logger.info(TAG, "Stopped continuous memory monitoring")
    }

    // ActivityManager-based memory monitoring
    private fun checkMemoryStatus() {
        application?.let { app ->
            val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.let { am ->
                val memoryInfo = ActivityManager.MemoryInfo()
                am.getMemoryInfo(memoryInfo)

                // Check if memory is low
                if (memoryInfo.lowMemory) {
                    logger.info(TAG, "Application detected low memory condition (available: ${memoryInfo.availMem / (1024 * 1024)}MB, threshold: ${memoryInfo.threshold / (1024 * 1024)}MB)")
                    handler?.onMemoryWarning()
                }
            }
        }
    }

    /**
     * Returns the current battery level as a value between 0.0 and 1.0
     * @return Battery level (0.0 = empty, 1.0 = full), or null if unable to determine
     */
    override fun getBatteryLevel(): Float? {
        return application?.let { app ->
            val batteryManager = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.let { bm ->
                val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (batteryLevel in 0..100) {
                    // Convert from percentage (0-100) to fraction (0.0-1.0)
                    batteryLevel / 100.0f
                } else {
                    logger.warn(TAG, "Invalid battery level from BatteryManager: $batteryLevel")
                    null
                }
            } ?: run {
                logger.warn(TAG, "BatteryManager service not available")
                null
            }
        } ?: run {
            logger.warn(TAG, "Application context not available for battery level check")
            null
        }
    }

    /**
     * Returns the current battery state
     * @return Battery state (CHARGING, DISCHARGING, NOT_CHARGING, FULL, UNKNOWN)
     */
    override fun getBatteryState(): BatteryState {
        return application?.let { app ->
            val batteryManager = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.let { bm ->
                val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> BatteryState.CHARGING
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryState.DISCHARGING
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryState.NOT_CHARGING
                    BatteryManager.BATTERY_STATUS_FULL -> BatteryState.FULL
                    BatteryManager.BATTERY_STATUS_UNKNOWN -> BatteryState.UNKNOWN
                    else -> {
                        logger.warn(TAG, "Unknown battery status from BatteryManager: $status")
                        BatteryState.UNKNOWN
                    }
                }
            } ?: run {
                logger.warn(TAG, "BatteryManager service not available")
                BatteryState.UNKNOWN
            }
        } ?: run {
            logger.warn(TAG, "Application context not available for battery state check")
            BatteryState.UNKNOWN
        }
    }

    /**
     * Checks whether Android's Battery Saver mode (a.k.a. Low Power Mode) is currently enabled.
     *
     * @return true if Battery Saver mode is on, false otherwise.
     */
    override fun isBatterySaverOn(): Boolean {
        return application?.let { app ->
            val powerManager = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode ?: false
        } ?: run {
            logger.warn(TAG, "Application context not available for Battery Saver check")
            false
        }
    }

    /**
     * Callback from NetworkMonitor indicating a change in the network connection type.
     */
    override fun onConnectionTypeChanged(connectionType: ConnectionType) {
        logger.info("DefaultEventAnalyticsController", "Network connection type changed: $connectionType")
        val type = NetworkConnectionType.fromWebRTCConnectionType(connectionType)
        handler?.onNetworkConnectionTypeChanged(type)
    }
}
