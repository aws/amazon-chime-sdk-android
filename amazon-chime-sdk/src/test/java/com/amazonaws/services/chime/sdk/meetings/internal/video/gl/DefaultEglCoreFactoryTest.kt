package com.amazonaws.services.chime.sdk.meetings.internal.video.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DefaultEglCoreFactoryTest {
    @MockK
    private lateinit var mockDisplay: EGLDisplay

    @MockK
    private lateinit var mockContext: EGLContext

    @MockK
    private var rootEglCore: EglCore? = null

    @MockK
    private lateinit var mockConfig: EGLConfig

    @MockK
    private lateinit var mockDefaultEglCore: DefaultEglCore

    @MockK
    private var refCountDelegate: RefCountDelegate? = null

    @InjectMockKs
    private lateinit var factory: DefaultEglCoreFactory

    @Before
    fun setUp() {
        mockkStatic(EGL14::class)
        mockkConstructor(DefaultEglCore::class)
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY) } returns mockDisplay
        every { EGL14.eglInitialize(any(), any(), any(), any(), any()) } returns true
        every { EGL14.eglQueryContext(any(), any(), any(), any(), any()) } returns true
        val slot = slot<Array<EGLConfig>>()
        every { EGL14.eglChooseConfig(any(), any(), any(), capture(slot), any(), any(), any(), any()) } answers {
            slot.captured[0] = mockConfig
            true
        }
        factory = DefaultEglCoreFactory(mockContext)
    }

    @Test
    fun `createEglCore should create rootEglCore`() {
        factory.createEglCore()

        assertNotNull(rootEglCore)
    }
}
