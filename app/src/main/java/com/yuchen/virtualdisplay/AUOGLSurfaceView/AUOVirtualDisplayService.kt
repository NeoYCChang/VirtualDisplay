package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.R.attr.name
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
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
import android.view.InputEvent
import android.view.MotionEvent
import android.view.WindowManager
import com.yuchen.virtualdisplay.AUOScreenMirroring.AUOMirrorManager
import java.lang.reflect.Method


class AUOVirtualDisplayService : Service() {

    private var m_MediaProjection: MediaProjection? = null
    private var m_virtual_display: VirtualDisplay? = null
    private val MEDIA_PROJECTION_CALLBACK: MediaProjection.Callback = object : MediaProjection.Callback() {}
    private val m_AUOGLSurfaceViews = mutableMapOf<WindowManager, ArrayList<AUOGLSurfaceView?>>()
    private var m_PrimarySurfaceView: AUOGLSurfaceView? = null
    val m_isMirror: Boolean = false
    private var m_displayWidth = 1
    private var m_displayHeight = 1
    private var m_injectInputEventMethod : Method? = null
    private var m_motionSetDisplayIdMethod : Method? = null
    private var m_mainHandler : Handler? = null
    private var m_inputManager: InputManager? = null
    private var m_virtualDisplayID = 0
    private var m_auoMirrorManager: AUOMirrorManager? = null
    private var m_auoMirrorSurfaceView: AUOGLSurfaceView? = null

    private val m_tag = "AUOVirtualDisplayService"
    private val m_dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var primary = intent.getBooleanExtra("primary", false)
            var displayID = intent.getIntExtra("displayID", -1)
            var displayWidth = intent.getIntExtra("displayWidth", 960)
            var displayHeight = intent.getIntExtra("displayHeight", 540)
            var viewWidth = intent.getIntExtra("viewWidth", 960)
            var viewHeight = intent.getIntExtra("viewHeight", 540)
            var viewX = intent.getIntExtra("viewX", 0)
            var viewY = intent.getIntExtra("viewY", 0)
            var textureCropWidth = intent.getIntExtra("textureCropWidth", 960)
            var textureCropHeight = intent.getIntExtra("textureCropHeight", 540)
            var textureOffsetX = intent.getIntExtra("textureOffsetX", 0)
            var textureOffsetY = intent.getIntExtra("textureOffsetY", 0)
            var isDeWarp = intent.getBooleanExtra("isDeWarp", false)
            if(primary){
                createProjectionVirtualDisplay(displayID, displayWidth, displayHeight, viewWidth, viewHeight, viewX, viewY,
                    textureOffsetX, textureOffsetY, textureCropWidth, textureCropHeight, isDeWarp)
            }
            else
            {
                createProjectionVirtualView(displayID, viewWidth, viewHeight, viewX, viewY,
                    textureOffsetX, textureOffsetY, textureCropWidth, textureCropHeight, isDeWarp)
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

//        if (viewWidth != null && viewHeight != null && displayID != null) {
//            createProjectionVirtualDisplay(displayID, viewWidth, viewHeight)
//        }
//        else{
//            createProjectionVirtualDisplay(0, 960, 540)
//        }

        if(m_injectInputEventMethod == null) {
            // 获取InputManager类
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")

            // 获取injectInputEvent方法的Method对象
            m_injectInputEventMethod = inputManagerClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
        }

        if(m_mainHandler == null){
            m_mainHandler = Handler(Looper.getMainLooper())
        }
        if(m_inputManager == null){
            m_inputManager = getSystemService(INPUT_SERVICE) as InputManager
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun createProjectionVirtualDisplay(displayid: Int, displaywidth: Int, displayheight: Int, viewwidth: Int, viewheight: Int, viewx: Int, viewy: Int,
                                       textureOffsetX: Int, textureOffsetY: Int, textureCropWidth: Int, textureCropHeight: Int, isdewarp: Boolean) {
        val display_manager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.getDefaultDisplay().getRealMetrics(dm)

        m_displayWidth = displaywidth
        m_displayHeight = displayheight

        m_PrimarySurfaceView = AUOGLSurfaceView(
            this,
            -1,
            null,
            displaywidth,
            displayheight,
            isdewarp
        )
        m_PrimarySurfaceView!!.setTextureCrop(textureOffsetX, textureOffsetY, textureCropWidth, textureCropHeight)
        // this callback will be invoked after m_AUOGLSurfaceView has been initialized
        val AUOGLSurfaceViewCallback = object : AUOGLSurfaceView.AUOGLSurfaceViewCallback {
            override fun onSurfaceCreatedCallback() {
                m_virtual_display = display_manager.createVirtualDisplay(
                    "testvirtual",
                    displaywidth,
                    displayheight,
                    dm.densityDpi,
                    m_PrimarySurfaceView?.getSurfceOfTexture(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                )
                m_virtualDisplayID = m_virtual_display!!.display.displayId

                m_mainHandler!!.post {
                    createMirrorVirtualView(viewwidth,viewheight,textureOffsetX,textureOffsetY,
                        textureCropWidth,textureCropHeight,isdewarp)
                }

//                val packageName =
//                    "com.example.auocid"; // Replace with the actual package name of the app you want to open
//                val intent: Intent? = getPackageManager().getLaunchIntentForPackage(packageName)
//                val options = ActivityOptions.makeBasic().setLaunchDisplayId(m_virtualDisplayID)
//                if (intent != null) {
//                    intent!!.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(intent, options.toBundle());
//                }

                //https://stackoverflow.com/questions/77524988/why-i-am-not-able-to-start-activity-on-virtualdisplay-created-in-the-same-applic
                //https://blog.csdn.net/Sunxiaolin2016/article/details/117666719
                //permissions: https://blog.csdn.net/Sunxiaolin2016/article/details/117666719
//                val package_name = "com.YourCompany.AndroidQuickStart"
//                val activity_path = "com.epicgames.unreal.GameActivity"
//                val intent2 = Intent()
//                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                val comp = ComponentName(package_name, activity_path)
//                intent2.setComponent(comp);
//                val options : ActivityOptions = ActivityOptions.makeBasic()
//                options.launchDisplayId = m_virtualDisplayID  // Here, fill in the DisplayId you want to specify.
//                startActivity(intent2, options.toBundle())


//                // Create a DOWN event
//                val downEvent = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(),
//                    MotionEvent.ACTION_DOWN, 100f, 200f, 0)
//
//                // Create a MOVE event
//                val moveEvent = MotionEvent.obtain(downEvent) // Clone the DOWN event timestamp
//                moveEvent.action = MotionEvent.ACTION_MOVE
//                moveEvent.setLocation(200f, 300f) // New position during move
//
//                // Create an UP event
//                val upEvent = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(),
//                    MotionEvent.ACTION_UP, 200f, 300f, 0)
//
//                injectInputEventMethod!!.invoke(inputManager, downEvent, 0)
//                injectInputEventMethod!!.invoke(inputManager, moveEvent, 0)
//                injectInputEventMethod!!.invoke(inputManager, upEvent, 0)
//


//                // Create a MotionEvent of type ACTION_DOWN (finger touching the screen)
//                val downTime = System.currentTimeMillis()  // The time the event started
//                val eventTime = System.currentTimeMillis()  // The time the event occurred
//                val x = 100f  // X-coordinate of the touch
//                val y = 200f  // Y-coordinate of the touch
//                val pointerId = 0  // Identifier for the touch pointer (useful for multitouch)
//
//                val motionevent: MotionEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
//
//                // 调用injectInputEvent方法，参数为InputEvent对象和注入模式
//                injectInputEventMethod.invoke(inputManager, motionevent, 0)
//                val virtualDisplayContext: Context = createDisplayContext(m_virtual_display!!.display)
//                val displayInputManager = virtualDisplayContext.getSystemService(INPUT_SERVICE)


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

            override fun onTouchCallback(motionEvent: MotionEvent) {
                m_mainHandler?.post {
                    injectMotionEvent(motionEvent, m_virtualDisplayID)
                }
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

    /**
     * Injects a MotionEvent into the input event stream by setting its display ID and invoking the input manager.
     *
     * This method uses reflection to access the private `setDisplayId` method of the `MotionEvent` class
     * and injects the MotionEvent into the input manager, simulating a touch event with a specified display ID.
     *
     * @param motionEvent the MotionEvent to be injected, which represents a touch or input event.
     * @param displayid the ID of the display to associate the MotionEvent with, used to specify which screen the event is for.
     */
    private fun injectMotionEvent(motionEvent: MotionEvent, displayid: Int) {
        if(m_motionSetDisplayIdMethod ==  null) {
            // Get the MotionEvent class
            val motionEventClass = MotionEvent::class.java
            m_motionSetDisplayIdMethod = motionEventClass.getMethod(
                "setDisplayId",
                Int::class.java      // DisplayID
            )
        }
        m_motionSetDisplayIdMethod!!.invoke(motionEvent, displayid)
        m_injectInputEventMethod?.invoke(m_inputManager, motionEvent, 0)
    }

    fun createProjectionVirtualView(displayid: Int, viewwidth: Int, viewheight: Int, viewx: Int, viewy: Int,
                                    textureOffsetX: Int, textureOffsetY: Int, textureCropWidth: Int, textureCropHeight: Int, isdewarp: Boolean) {
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
                m_displayWidth,
                m_displayHeight,
                isdewarp
            )
        }
        else
        {
            Log.d(m_tag,"create VirtualView with m_PrimarySurfaceView")
            AUOGLSurfaceView = AUOGLSurfaceView(
                this,
                m_PrimarySurfaceView!!.getTextureID(),
                m_PrimarySurfaceView?.getAUOSurfaceTexture(),
                m_displayWidth,
                m_displayHeight,
                isdewarp
            )
            AUOGLSurfaceView?.setSurfaceAndEglContext(null, m_PrimarySurfaceView?.getEglContext())
        }
        val AUOGLSurfaceViewCallback = object : AUOGLSurfaceView.AUOGLSurfaceViewCallback {
            override fun onSurfaceCreatedCallback() {
            }

            override fun onTouchCallback(motionEvent: MotionEvent) {
                m_mainHandler?.post {
                    injectMotionEvent(motionEvent, m_virtualDisplayID)
                }
            }
        }
        AUOGLSurfaceView?.setAUOGLSurfaceViewCallback(AUOGLSurfaceViewCallback)


        AUOGLSurfaceView!!.setTextureCrop(textureOffsetX, textureOffsetY, textureCropWidth, textureCropHeight)


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

    fun createMirrorVirtualView(viewwidth: Int, viewheight: Int,
                                    textureOffsetX: Int, textureOffsetY: Int, textureCropWidth: Int, textureCropHeight: Int, isdewarp: Boolean) {

        if(m_PrimarySurfaceView != null)
        {
            Log.d(m_tag,"create Mirror VirtualView with m_PrimarySurfaceView")
            m_auoMirrorSurfaceView = AUOGLSurfaceView(
                this,
                m_PrimarySurfaceView!!.getTextureID(),
                m_PrimarySurfaceView?.getAUOSurfaceTexture(),
                m_displayWidth,
                m_displayHeight,
                isdewarp
            )
            m_auoMirrorManager = AUOMirrorManager(viewwidth, viewheight)
            m_auoMirrorSurfaceView!!.setSurfaceAndEglContext(m_auoMirrorManager!!.getSurface(), m_PrimarySurfaceView?.getEglContext())
            m_auoMirrorSurfaceView!!.setTextureCrop(textureOffsetX, textureOffsetY, textureCropWidth, textureCropHeight)
            m_auoMirrorSurfaceView!!.auosurfaceCreated(null)

            m_auoMirrorManager!!.start()
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
        m_auoMirrorManager?.close()
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