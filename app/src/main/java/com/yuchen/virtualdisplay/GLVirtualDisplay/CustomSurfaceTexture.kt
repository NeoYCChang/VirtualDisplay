package com.yuchen.virtualdisplay.GLVirtualDisplay

import android.graphics.SurfaceTexture
import android.util.Log


// Define a custom SurfaceTexture class, inheriting from SurfaceTexture
class CustomSurfaceTexture(textureId: Int) : SurfaceTexture(textureId) {

    // Companion object to define static-like functions and properties
    companion object {
        // Static-like function
    }
    // A list to hold all event listeners
    private val listeners = mutableListOf<() -> Unit>()

    init {
        setOnFrameAvailableListener(object : SurfaceTexture.OnFrameAvailableListener {
            override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
                triggerEvent();
                Log.d("MainActivity", "Frame available: processing frame")
            }
        })
    }

    // Add a listener
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    // Trigger the event (calls all listeners)
    fun triggerEvent() {
        listeners.forEach { it() }
    }


}
