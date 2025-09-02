/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.ingestion

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultAppStateMonitor(
    private val logger: Logger,
    private val application: Application? = null,
    private val memoryCheckIntervalMs: Long = 5000L // Check memory every 5 seconds
) : AppStateMonitor, DefaultLifecycleObserver {

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

    override fun bindHandler(handler: AppStateHandler) {
        this.handler = handler
    }

    override fun start() {
        // Prevent registering self as an observer multiple times
        stop()

        shouldPostEvent = true

        // Register for process lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Start continuous memory monitoring
        startMemoryMonitoring()

        logger.info(TAG, "Started monitoring app state and memory")
    }

    override fun stop() {
        shouldPostEvent = false

        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)

        // Stop continuous memory monitoring
        stopMemoryMonitoring()

        logger.info(TAG, "Stopped monitoring app state and memory")
    }

    // ProcessLifecycleOwner callbacks
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
}
