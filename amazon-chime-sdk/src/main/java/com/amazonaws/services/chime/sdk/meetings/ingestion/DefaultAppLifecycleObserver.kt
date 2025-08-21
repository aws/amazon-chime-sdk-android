package com.amazonaws.services.chime.sdk.meetings.ingestion

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultAppLifecycleObserver(
    private val eventAnalyticsController: EventAnalyticsController,
    private val logger: Logger
) : AppLifecycleObserver, DefaultLifecycleObserver {

    private val TAG = "DefaultAppLifecycleObserver"

    override fun startObserving() {
        logger.info(TAG, "Start observing app lifecycle")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stopObserving() {
        logger.info(TAG, "Stop observing app lifecycle")
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        logger.info(TAG, "App entered foreground")
        eventAnalyticsController.pushHistory(MeetingHistoryEventName.appEnteredForeground)
    }

    override fun onStop(owner: LifecycleOwner) {
        logger.info(TAG, "App entered background")
        eventAnalyticsController.pushHistory(MeetingHistoryEventName.appEnteredBackground)
    }
}
