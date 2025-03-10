package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.opengl.EGL14
import android.view.Surface
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface


/**
 * 1. Get the EGL instance:
 * 2. Get the default display device (the window):
 * 3. Initialize the default display device:
 * 4. Set the attributes for the display device:
 * 5. Get the corresponding configuration with the specified attributes from the system:
 * 6. Create the EGLContext:
 * 7. Create the rendering Surface:
 * 8. Bind the EGLContext and Surface to the display device:
 * 9. Refresh the data and display the rendered scene:
 */
class EGLHelper {
    private var mEgl: EGL10? = null
    private var mEglDisplay: EGLDisplay? = null
    private var mEglContext: EGLContext? = null
    private var mEglSurface: EGLSurface? = null
    fun initEgl(surface: Surface?, eglContext: EGLContext?) {
        // 1. Get an EGL instance
        mEgl = EGLContext.getEGL() as EGL10
        // 2. Get the default display device (the window)
        mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }
        // 3. Initialize the default display device
        val version = IntArray(2)
        if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
            throw RuntimeException("eglInitialize failed")
        }
        // 4. Set the attributes for the display device
        val attrbutes = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 8,
            EGL10.EGL_STENCIL_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_NONE
        )
        val num_config = IntArray(1)
        require(
            mEgl!!.eglChooseConfig(
                mEglDisplay,
                attrbutes,
                null,
                1,
                num_config
            )
        ) { "eglChooseConfig failed" }
        val numConfigs = num_config[0]
        require(numConfigs > 0) { "No configs match configSpec" }
        // 5. Get the configuration with the corresponding attributes from the system
        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        require(
            mEgl!!.eglChooseConfig(
                mEglDisplay, attrbutes, configs, numConfigs,
                num_config
            )
        ) { "eglChooseConfig#2 failed" }
        // 6. Create an EGL context
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        mEglContext = if (eglContext != null) {
            mEgl!!.eglCreateContext(mEglDisplay, configs[0], eglContext, attrib_list)
        } else {
            mEgl!!.eglCreateContext(
                mEglDisplay,
                configs[0], EGL10.EGL_NO_CONTEXT, attrib_list
            )
        }
        // 7. Create the rendering surface
        mEglSurface = mEgl!!.eglCreateWindowSurface(mEglDisplay, configs[0], surface, null)
        // 8. Bind the EGL context and surface to the display device
        if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw RuntimeException("eglMakeCurrent fail")
        }
    }

    /**
     * Swap buffers
     * Manual refresh
     *
     * @return
     */
    fun swapBuffers(): Boolean {
        if (mEgl != null) {
            return mEgl!!.eglSwapBuffers(mEglDisplay, mEglSurface)
        } else {
            throw RuntimeException("egl is null")
        }
    }

    fun getmEglContext(): EGLContext? {
        return mEglContext
    }

    fun destoryEgl() {
        if (mEgl != null) {
            // Unbind
            mEgl!!.eglMakeCurrent(
                mEglDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT
            )
            // Set surface to null
            mEgl!!.eglDestroySurface(mEglDisplay, mEglSurface)
            mEglSurface = null
            // Set context to null
            mEgl!!.eglDestroyContext(mEglDisplay, mEglContext)
            mEglContext = null
            // Deactivate the display device
            mEgl!!.eglTerminate(mEglDisplay)
            mEglDisplay = null
            mEgl = null
        }
    }
}