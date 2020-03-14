/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media

import com.amazon.chime.sdk.media.devicecontroller.DeviceController
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.activespeakerdetector.ActiveSpeakerDetectorFacade
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileControllerFacade

interface AudioVideoFacade : AudioVideoControllerFacade, RealtimeControllerFacade, DeviceController,
    VideoTileControllerFacade, ActiveSpeakerDetectorFacade
