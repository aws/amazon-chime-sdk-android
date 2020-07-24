/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.model

import androidx.lifecycle.ViewModel
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSession
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionCredentials

class MeetingSessionModel : ViewModel() {
    private lateinit var meetingSession: MeetingSession

    fun setMeetingSession(meetingSession: MeetingSession) {
        this.meetingSession = meetingSession
    }

    val credentials: MeetingSessionCredentials
        get() = meetingSession.configuration.credentials

    val audioVideo: AudioVideoFacade
        get() = meetingSession.audioVideo
}
