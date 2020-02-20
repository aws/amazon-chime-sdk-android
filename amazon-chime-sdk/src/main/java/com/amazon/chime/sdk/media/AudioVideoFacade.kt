/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media

import com.amazon.chime.sdk.media.devicecontroller.DeviceController
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoControllerFacade
import com.amazon.chime.sdk.media.mediacontroller.RealtimeControllerFacade

interface AudioVideoFacade : AudioVideoControllerFacade, RealtimeControllerFacade, DeviceController
