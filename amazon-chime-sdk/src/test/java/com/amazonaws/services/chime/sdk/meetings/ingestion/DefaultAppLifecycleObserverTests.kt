package com.amazonaws.services.chime.sdk.meetings.ingestion

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultAppLifecycleObserverTests {

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockEventAnalyticsController: EventAnalyticsController

    private lateinit var lifecycleRegistry: LifecycleRegistry

    @InjectMockKs
    private lateinit var appLifecycleObserver: DefaultAppLifecycleObserver

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkObject(ProcessLifecycleOwner)

        // Create a dummy LifecycleOwner
        val dummyOwner = mockk<LifecycleOwner>(relaxed = true)
        lifecycleRegistry = LifecycleRegistry(dummyOwner)

        // Mock the lifecycle property
        every { ProcessLifecycleOwner.get().lifecycle } returns lifecycleRegistry

        appLifecycleObserver = DefaultAppLifecycleObserver(mockEventAnalyticsController, mockLogger)
    }

    @Test
    fun `startObserving should add observer`() {
        appLifecycleObserver.startObserving()

        assert(lifecycleRegistry.observerCount == 1)
    }

    @Test
    fun `stopObserving should remove observer`() {
        appLifecycleObserver.startObserving()
        appLifecycleObserver.stopObserving()

        assert(lifecycleRegistry.observerCount == 0)
    }

    @Test
    fun `onStart should log info and push appEnteredForeground event`() {
        appLifecycleObserver.onStart(mockk(relaxed = true))

        verify {
            mockEventAnalyticsController.pushHistory(MeetingHistoryEventName.appEnteredForeground)
        }
    }

    @Test
    fun `onStop should log info and push appEnteredBackground event`() {
        appLifecycleObserver.onStop(mockk(relaxed = true))

        verify {
            mockEventAnalyticsController.pushHistory(MeetingHistoryEventName.appEnteredBackground)
        }
    }
}
