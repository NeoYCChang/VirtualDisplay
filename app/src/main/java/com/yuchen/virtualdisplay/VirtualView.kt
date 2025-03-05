package com.yuchen.virtualdisplay

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.Surface;
import android.view.SurfaceView

interface SurfaceCallback {
    fun onSurfaceCreated(holder: SurfaceHolder)
    fun onSurfaceDestroyed(holder: SurfaceHolder)
}

class VirtualView(context: Context) : SurfaceView(context) {
    private var surfaceCallback: SurfaceCallback? = null

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceCallback?.onSurfaceCreated(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // 可根据需要实现
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceCallback?.onSurfaceDestroyed(holder)
            }
        })
//        holder.setFixedSize(5499, 1000)
    }

    // 设置回调
    fun setSurfaceCallback(callback: SurfaceCallback) {
        surfaceCallback = callback
    }

    fun getSurface(): Surface {
        return holder.surface
    }
}