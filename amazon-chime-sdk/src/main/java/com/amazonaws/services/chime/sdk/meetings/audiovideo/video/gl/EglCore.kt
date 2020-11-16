/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface

/**
 * [EglCore] is an interface for containing all EGL state in one component. In the future it may contain additional helper methods.
 */
interface EglCore {
    /**
     * A [EGLContext] which was created with [eglDisplay] and [eglConfig], may or may not be the current context on the thread, users
     * must call [EGL14.eglMakeCurrent] after creating a valid current surface. This may be passed to other components to share the context.
     */
    val eglContext: EGLContext

    /**
     * Current initialized [EGLDisplay]
     */
    val eglDisplay: EGLDisplay

    /**
     * Current used [EGLConfig]
     */
    val eglConfig: EGLConfig

    /**
     * Current [EGLSurface]. Will likely be [EGL14.EGL_NO_SURFACE] on init. As [EglCore] does not include helper functions
     * users must create this value themselves, which is why it is defined as `var`
     */
    var eglSurface: EGLSurface

    /**
     * Discards all resources held by this class, notably the EGL context. This must be
     * called from the thread where the context was created.
     *
     * On completion, no context will be current.
     */
    fun release()
}
