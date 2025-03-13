package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.graphics.SurfaceTexture
import android.util.Log

// Define a custom SurfaceTexture class, inheriting from SurfaceTexture
class AUOSurfaceTexture(textureId: Int) : SurfaceTexture(textureId) {

    // Companion object to define static-like functions and properties
    companion object {
        // Static-like function
    }
    // A list to hold all event listeners
    private val listeners = mutableListOf<() -> Unit>()
    private val m_tag = "AUOSurfaceTexture"

    init {
        setOnFrameAvailableListener(object : SurfaceTexture.OnFrameAvailableListener {
            override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
                triggerEvent();
                //Log.d("MainActivity", "Frame available: processing frame")
            }
        })
    }

    // Add a listener
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    // Trigger the event (calls all listeners)
    fun triggerEvent() {
        //Log.d(m_tag,"triggerEvent")
        listeners.forEach { it() }
    }


}