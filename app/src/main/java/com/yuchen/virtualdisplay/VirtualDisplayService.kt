package com.yuchen.virtualdisplay

import android.R.attr.name
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.WindowManager
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUOVirtualDisplayService
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUOVirtualDisplayService.Companion


class VirtualDisplayService : Service() {

    private var m_MediaProjection: MediaProjection? = null
    private var m_virtual_display: VirtualDisplay? = null
    private var m_projection_view: VirtualView? = null
    private var m_displayWindowManager: WindowManager? = null
    private val MEDIA_PROJECTION_CALLBACK: MediaProjection.Callback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            // Handle media projection stop here
            m_virtual_display?.release()
            m_virtual_display = null
        }
    }
    val m_isMirror: Boolean = true

    companion object {
        // static variable
        var resultCode: Int = 0
        var resultData: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
        startMediaProjectionForeground()

        val MEDIA_PROJECTION_MANAGER =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        m_MediaProjection =
            AUOVirtualDisplayService.resultData?.let { MEDIA_PROJECTION_MANAGER.getMediaProjection(
                AUOVirtualDisplayService.resultCode, it) }
        m_MediaProjection?.registerCallback(MEDIA_PROJECTION_CALLBACK, null)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle: Bundle? = intent?.extras
        val viewWidth = bundle?.getInt("viewWidth")
        val viewHeight = bundle?.getInt("viewHeight")
        val displayID = bundle?.getInt("displayID")

        if (viewWidth != null && viewHeight != null && displayID != null) {
            createProjectionVirtualDisplay(displayID, viewWidth, viewHeight)
        }
        else{
            createProjectionVirtualDisplay(0, 960, 540)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun createProjectionVirtualDisplay(displayid: Int, viewwidth: Int, viewheight: Int) {
        m_projection_view = VirtualView(this)
        val display_manager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.getDefaultDisplay().getRealMetrics(dm);

        val surfaceCallback = object : SurfaceCallback {
            override fun onSurfaceCreated(holder: SurfaceHolder) {
                //  mirror primary display
                if(m_isMirror) {
                    m_virtual_display = m_MediaProjection?.createVirtualDisplay(
                        "testvirtual",
                        viewwidth,
                        viewheight,
                        dm.densityDpi,
                        Display.FLAG_ROUND,
                        m_projection_view!!.getSurface(),
                        null,
                        null
                    )
                }
                else {
                    m_virtual_display = display_manager.createVirtualDisplay(
                        "testvirtual",
                        viewwidth,
                        viewheight,
                        dm.densityDpi,
                        m_projection_view!!.getSurface(),
                        0
                    )
                }
            }

            override fun onSurfaceDestroyed(holder: SurfaceHolder) {
                // Handle surface destruction
            }
        }

        m_projection_view!!.setSurfaceCallback(surfaceCallback)


//        val displays: Array<Display> = display_manager.displays
//        for (display in displays) {
//            val displayId = display.displayId
//            println("DisplayId: $displayId")
//            val displayContext: Context = createDisplayContext(display)
//            val displayWindowManager =
//                displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
//            val PROJECTION_VIEW_PARAMS: WindowManager.LayoutParams = newLayoutParams()
////            PROJECTION_VIEW_PARAMS.width = 300
////            PROJECTION_VIEW_PARAMS.height = 500
//            //displayWindowManager.addView(projection_view, PROJECTION_VIEW_PARAMS)
//            Log.d("debug","debug1")
//        }
        val display : Display = display_manager.getDisplay(displayid)
        val displayContext: Context = createDisplayContext(display)
        m_displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val PROJECTION_VIEW_PARAMS: WindowManager.LayoutParams = newLayoutParams()
        PROJECTION_VIEW_PARAMS.width = viewwidth
        PROJECTION_VIEW_PARAMS.height = viewheight
        m_displayWindowManager!!.addView(m_projection_view, PROJECTION_VIEW_PARAMS)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("test","onDestroy")
        m_virtual_display?.release();
        m_virtual_display = null;
        MEDIA_PROJECTION_CALLBACK?.let { m_MediaProjection?.unregisterCallback(it) };
        m_MediaProjection?.stop();
        m_MediaProjection = null;
        m_displayWindowManager?.removeViewImmediate(m_projection_view)
        this.stopForeground(true)
    }

    private fun newLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams()
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        params.gravity = Gravity.START or Gravity.TOP
        params.format = PixelFormat.TRANSLUCENT
        return params
    }

    @SuppressLint("ForegroundServiceType")
    fun startMediaProjectionForeground() {
        val notificationBuilder: Notification.Builder = Notification.Builder(this)
            .setContentTitle(name.toString() + "服务已启动")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ID_MEDIA_PROJECTION",
                "屏幕录制",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            notificationBuilder.setChannelId("CHANNEL_ID_MEDIA_PROJECTION")
        }
        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            this.startForeground(1, notification)
        }
    }
}