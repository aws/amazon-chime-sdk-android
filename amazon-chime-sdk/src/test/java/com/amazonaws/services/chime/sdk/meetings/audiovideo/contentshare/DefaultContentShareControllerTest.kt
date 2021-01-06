/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.contentshare

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource
import com.amazonaws.services.chime.sdk.meetings.internal.contentshare.ContentShareVideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultContentShareControllerTest {
    @MockK
    private lateinit var videoSource: VideoSource

    @MockK
    private lateinit var contentShareObserver: ContentShareObserver

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var contentShareVideoClientController: ContentShareVideoClientController

    @InjectMockKs
    private lateinit var contentShareController: DefaultContentShareController

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `startContentShare should call contentShareVideoClientController startVideoShare when video source is available`() {
        val contentShareSource = ContentShareSource()
        contentShareSource.videoSource = videoSource

        contentShareController.startContentShare(contentShareSource)

        verify(exactly = 1) { contentShareVideoClientController.startVideoShare(videoSource) }
    }

    @Test
    fun `startContentShare should not call contentShareVideoClientController startVideoShare when video source is null`() {
        val contentShareSource = ContentShareSource()

        contentShareController.startContentShare(contentShareSource)

        verify(exactly = 0) { contentShareVideoClientController.startVideoShare(any()) }
    }

    @Test
    fun `stopContentShare should call contentShareVideoClientController stopVideoShare`() {
        contentShareController.stopContentShare()

        verify(exactly = 1) { contentShareVideoClientController.stopVideoShare() }
    }

    @Test
    fun `addContentShareObserver should call contentShareVideoClientController subscribeToVideoClientStateChange with given observer`() {
        contentShareController.addContentShareObserver(contentShareObserver)

        verify(exactly = 1) { contentShareVideoClientController.subscribeToVideoClientStateChange(contentShareObserver) }
    }

    @Test
    fun `removeContentShareObserver should call contentShareVideoClientController unsubscribeFromVideoClientStateChange with given observer`() {
        contentShareController.removeContentShareObserver(contentShareObserver)

        verify(exactly = 1) { contentShareVideoClientController.unsubscribeFromVideoClientStateChange(contentShareObserver) }
    }
}
