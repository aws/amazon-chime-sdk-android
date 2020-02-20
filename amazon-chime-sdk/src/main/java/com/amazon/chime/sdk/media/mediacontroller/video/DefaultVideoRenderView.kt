package com.amazon.chime.sdk.media.mediacontroller.video

import android.content.Context
import android.util.AttributeSet
import com.amazon.chime.webrtc.SurfaceViewRenderer

class DefaultVideoRenderView : SurfaceViewRenderer {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
}
