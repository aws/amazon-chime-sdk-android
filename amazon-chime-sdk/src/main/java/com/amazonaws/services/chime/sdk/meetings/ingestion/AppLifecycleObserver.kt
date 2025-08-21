package com.amazonaws.services.chime.sdk.meetings.ingestion

interface AppLifecycleObserver {
    fun startObserving()
    fun stopObserving()
}
