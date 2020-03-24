/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import android.content.Context
import android.util.AttributeSet
import com.amazon.chime.webrtc.EglBase
import com.amazon.chime.webrtc.SurfaceViewRenderer
import com.amazon.chime.webrtc.VideoRenderer

class DefaultVideoRenderView : SurfaceViewRenderer, VideoRenderView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun renderFrame(frame: Any) {
        this.renderFrame(frame as VideoRenderer.I420Frame)
    }

    override fun initialize(initParams: Any?) {
        this.init((initParams as EglBase).eglBaseContext, null)
    }

    override fun finalize() {
        this.release()
    }
}
