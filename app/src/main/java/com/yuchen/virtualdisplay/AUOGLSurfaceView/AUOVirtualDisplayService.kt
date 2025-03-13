package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.R.attr.name
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager


class AUOVirtualDisplayService : Service() {

    private var m_MediaProjection: MediaProjection? = null
    private var m_virtual_display: VirtualDisplay? = null
    private val MEDIA_PROJECTION_CALLBACK: MediaProjection.Callback = object : MediaProjection.Callback() {}
    private val m_AUOGLSurfaceViews = mutableMapOf<WindowManager, ArrayList<AUOGLSurfaceView?>>()
    private var m_PrimarySurfaceView: AUOGLSurfaceView? = null
    val m_isMirror: Boolean = false
    private val m_tag = "AUOVirtualDisplayService"
    private val m_dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var primary = intent.getBooleanExtra("primary", false)
            var displayID = intent.getIntExtra("displayID", -1)
            var viewWidth = intent.getIntExtra("viewWidth", 960)
            var viewHeight = intent.getIntExtra("viewHeight", 540)
            var viewX = intent.getIntExtra("viewX", 0)
            var viewY = intent.getIntExtra("viewY", 0)
            if(primary){
                createProjectionVirtualDisplay(displayID, viewWidth, viewHeight, viewX, viewY)
            }
            else
            {
                createProjectionVirtualView(displayID, viewWidth, viewHeight, viewX, viewY)
            }

        }
    }

    companion object {
        // static variable
        var resultCode: Int = 0
        var resultData: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter("com.yuchen.virtualdisplay.UPDATE_DATA")
        registerReceiver(m_dataReceiver, filter, Context.RECEIVER_EXPORTED)
        startMediaProjectionForeground()
        //  mirror primary display
        if(m_isMirror) {
            val MEDIA_PROJECTION_MANAGER =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            m_MediaProjection =
                resultData?.let { MEDIA_PROJECTION_MANAGER.getMediaProjection(resultCode, it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle: Bundle? = intent?.extras
        val viewWidth = bundle?.getInt("viewWidth")
        val viewHeight = bundle?.getInt("viewHeight")
        val displayID = bundle?.getInt("displayID")

//        if (viewWidth != null && viewHeight != null && displayID != null) {
//            createProjectionVirtualDisplay(displayID, viewWidth, viewHeight)
//        }
//        else{
//            createProjectionVirtualDisplay(0, 960, 540)
//        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun createProjectionVirtualDisplay(displayid: Int, viewwidth: Int, viewheight: Int, viewx: Int, viewy: Int) {
        val display_manager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.getDefaultDisplay().getRealMetrics(dm)

        m_PrimarySurfaceView = AUOGLSurfaceView(
            this,
            -1,
            null,
            viewwidth,
            viewheight
        )
        val context = this
        val mainHandler = Handler(Looper.getMainLooper())

        // this callback will be invoked after m_AUOGLSurfaceView has been initialized
        val AUOGLSurfaceViewCallback = object : AUOGLSurfaceView.AUOGLSurfaceViewCallback {
            override fun onSurfaceCreatedCallback() {
                m_virtual_display = display_manager.createVirtualDisplay(
                    "testvirtual",
                    viewwidth,
                    viewheight,
                    dm.densityDpi,
                    m_PrimarySurfaceView?.getSurfceOfTexture(),
                    0
                )
                // create a second view mirrored by m_AUOGLSurfaceView
//                m_AUOGLSurfaceView2 = AUOGLSurfaceView(
//                    context,
//                    m_AUOGLSurfaceView!!.getTextureID(),
//                    m_AUOGLSurfaceView?.getAUOSurfaceTexture(),
//                    viewwidth,
//                    viewheight
//                )
//                if(m_AUOGLSurfaceView != null)
//                {
//                    Log.d("AUOVirtualDisplayService",  "m_AUOGLSurfaceView == null")
//                }
//                m_AUOGLSurfaceView2?.setSurfaceAndEglContext(null, m_AUOGLSurfaceView?.getEglContext())
//                mainHandler.post {
//                    // Display a second view on display:2
//                    val display : Display = display_manager.getDisplay(0)
//                    val displayContext: Context = createDisplayContext(display)
//                    val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
//                    val PROJECTION_VIEW_PARAMS: WindowManager.LayoutParams = newLayoutParams()
//                    PROJECTION_VIEW_PARAMS.width = viewwidth
//                    PROJECTION_VIEW_PARAMS.height = viewheight
//                    displayWindowManager!!.addView(m_AUOGLSurfaceView2, PROJECTION_VIEW_PARAMS)
//                }
            }
        }
        m_PrimarySurfaceView?.setAUOGLSurfaceViewCallback(AUOGLSurfaceViewCallback)



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
        val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val PROJECTION_VIEW_PARAMS: WindowManager.LayoutParams = newLayoutParams()
        PROJECTION_VIEW_PARAMS.width = viewwidth
        PROJECTION_VIEW_PARAMS.height = viewheight
        PROJECTION_VIEW_PARAMS.x = viewx
        PROJECTION_VIEW_PARAMS.y = viewy

        //After m_AUOGLSurfaceView is added to m_displayWindowManager, m_AUOGLSurfaceView will begin initialization.
        displayWindowManager!!.addView(m_PrimarySurfaceView, PROJECTION_VIEW_PARAMS)
        if( m_AUOGLSurfaceViews[displayWindowManager] == null) {
            m_AUOGLSurfaceViews[displayWindowManager] = ArrayList<AUOGLSurfaceView?>()
            m_AUOGLSurfaceViews[displayWindowManager]!!.add(m_PrimarySurfaceView)
        }
        else
        {
            m_AUOGLSurfaceViews[displayWindowManager]!!.add(m_PrimarySurfaceView)
        }
    }

    fun createProjectionVirtualView(displayid: Int, viewwidth: Int, viewheight: Int, viewx: Int, viewy: Int) {
        val display_manager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.getDefaultDisplay().getRealMetrics(dm)

        var AUOGLSurfaceView : AUOGLSurfaceView? = null

        if(m_PrimarySurfaceView == null)
        {
            Log.d(m_tag,"create VirtualView without m_PrimarySurfaceView")
            AUOGLSurfaceView = AUOGLSurfaceView(
                this,
                -1,
                null,
                viewwidth,
                viewheight
            )
        }
        else
        {
            Log.d(m_tag,"create VirtualView with m_PrimarySurfaceView")
            AUOGLSurfaceView = AUOGLSurfaceView(
                this,
                m_PrimarySurfaceView!!.getTextureID(),
                m_PrimarySurfaceView?.getAUOSurfaceTexture(),
                viewwidth,
                viewheight
            )
            AUOGLSurfaceView?.setSurfaceAndEglContext(null, m_PrimarySurfaceView?.getEglContext())
        }


        val display : Display = display_manager.getDisplay(displayid)
        val displayContext: Context = createDisplayContext(display)
        val displayWindowManager = displayContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val PROJECTION_VIEW_PARAMS: WindowManager.LayoutParams = newLayoutParams()
        PROJECTION_VIEW_PARAMS.width = viewwidth
        PROJECTION_VIEW_PARAMS.height = viewheight
        PROJECTION_VIEW_PARAMS.x = viewx
        PROJECTION_VIEW_PARAMS.y = viewy

        //After m_AUOGLSurfaceView is added to m_displayWindowManager, m_AUOGLSurfaceView will begin initialization.
        displayWindowManager!!.addView(AUOGLSurfaceView, PROJECTION_VIEW_PARAMS)
        if( m_AUOGLSurfaceViews[displayWindowManager] == null) {
            m_AUOGLSurfaceViews[displayWindowManager] = ArrayList<AUOGLSurfaceView?>()
            m_AUOGLSurfaceViews[displayWindowManager]!!.add(AUOGLSurfaceView)
        }
        else
        {
            m_AUOGLSurfaceViews[displayWindowManager]!!.add(AUOGLSurfaceView)
        }
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
        for ((window, views) in m_AUOGLSurfaceViews) {
            Log.d("removeViewImmediate","removeViewImmediate")
            views.forEach{ view ->
                window.removeViewImmediate(view)
                Log.d("removeViewImmediate","removeViewImmediate")
            }
        }
        unregisterReceiver(m_dataReceiver)
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