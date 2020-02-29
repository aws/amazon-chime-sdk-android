/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.mediacontroller.video

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
