/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext

/**
 * [DefaultEglCore] is an implementation of [EglCore] which uses EGL14 and OpenGLES2.
 * OpenGLES3 has incompatibilities with AmazonChimeSDKMedia library.
 */
class DefaultEglCore(
    private val releaseCallback: Runnable? = null,
    sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT
) : EglCore {
    override var eglContext = EGL14.EGL_NO_CONTEXT
    override var eglSurface = EGL14.EGL_NO_SURFACE
    override var eglDisplay = EGL14.EGL_NO_DISPLAY
    override lateinit var eglConfig: EGLConfig

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = null
            throw RuntimeException("Unable to initialize EGL14")
        }

        eglConfig = getConfig()

        // Use version 2 for compatibility with AmazonChimeSDKMedia library
        val attributeList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, eglConfig, sharedContext,
            attributeList, 0
        )

        // Confirm with query.
        val values = IntArray(1)
        if (!EGL14.eglQueryContext(
                eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0
            )
        ) {
            throw RuntimeException("Failed to query context")
        }
    }

    override fun release() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }

        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay. So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT

        releaseCallback?.run()
    }

    private fun getConfig(): EGLConfig {
        val attributeList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, // ES2 for compatability with AmazonChimeSDKMedia library
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, // On by default
            EGL14.EGL_NONE, 0,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attributeList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("Failed to choose config")
        }
        return configs[0] ?: throw RuntimeException("Config was null")
    }
}
