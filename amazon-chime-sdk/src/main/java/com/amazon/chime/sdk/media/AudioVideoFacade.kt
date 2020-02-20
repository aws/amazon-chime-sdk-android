package com.amazon.chime.sdk.media

import com.amazon.chime.sdk.media.devicecontroller.DeviceController
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.video.VideoTileControllerFacade

interface AudioVideoFacade : AudioVideoControllerFacade, RealtimeControllerFacade, DeviceController,
    VideoTileControllerFacade
