/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.audio.activespeakerdetector.ActiveSpeakerDetectorFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileControllerFacade
import com.amazonaws.services.chime.sdk.meetings.device.DeviceController
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeControllerFacade

interface AudioVideoFacade : AudioVideoControllerFacade,
    RealtimeControllerFacade,
    DeviceController,
    VideoTileControllerFacade, ActiveSpeakerDetectorFacade
