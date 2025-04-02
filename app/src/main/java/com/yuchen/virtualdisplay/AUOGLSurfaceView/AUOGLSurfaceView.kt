package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUORender.Texture_Size
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.microedition.khronos.egl.EGLContext
import kotlin.concurrent.read
import kotlin.concurrent.write


class AUOGLSurfaceView @JvmOverloads constructor(
    context: Context ,textureid : Int, surfaseTexture: AUOSurfaceTexture?, displayWidth : Int, displayHeight : Int, isDeWarp: Boolean
    ) :
    SurfaceView(context), SurfaceHolder.Callback {

    interface AUOGLSurfaceViewCallback {
        fun onSurfaceCreatedCallback()
        fun onTouchCallback(motionEvent: MotionEvent)
    }
    private var m_AUOGLSurfaceViewCallback: AUOGLSurfaceViewCallback? = null
    // The surface can be passed from outside
    private var surface: Surface? = null

    // EGL context
    private var eglContext: EGLContext? = null

    private var eglThread: EGLThread? = null

    // Rendering mode, manual refresh, auto-refresh. A default value is needed, otherwise it won't work; default is auto-refresh with 60 frames per second.
    private var renderMode = RenderMode.RENDERMODE_WHEN_DIRTY

    private var m_SurfaseTexture: AUOSurfaceTexture? = surfaseTexture
    private var m_textureid: Int = textureid
    private val m_displayWidth = displayWidth
    private val m_displayHeight = displayHeight
    private val m_context: Context = context
    private var m_SurfaceOfTexture: Surface? = null
    private var m_AUORender: AUORender?  = null
    private var m_isDeWarp: Boolean = isDeWarp
    private val m_tag = "AUOGLSurfaceView"
    private var m_SurfaseTexture_createdByThis = false
    private val mTextureSize : Texture_Size = Texture_Size(
        960, 540, 0, 0,
        960,540
    )
    /**
     * Set render mode
     * 0 manual refresh
     * 1 auto refresh
     */
    enum class RenderMode {
        RENDERMODE_WHEN_DIRTY,
        RENDERMODE_CONTINUOUSLY
    }

    fun setRender(eglRender: AUORender?) {
        this.m_AUORender = eglRender
    }

    fun setRenderMode(renderMode: RenderMode) {
        this.renderMode = renderMode
    }

    init {
        holder.addCallback(this)
    }

    fun setSurfaceAndEglContext(surface: Surface?, eglContext: EGLContext?) {
        this.surface = surface
        this.eglContext = eglContext
    }

    fun setTextureCrop(offsetX: Int, offsetY: Int, width: Int, height: Int){
        mTextureSize.offsetX = offsetX
        mTextureSize.offsetY = offsetY
        mTextureSize.cropWidth  = width
        mTextureSize.cropHeight = height
    }

    fun requestRender() {
        if (eglThread != null) {
            //Log.d(m_tag,"requestRender")
            eglThread!!.requestRender()
        }
    }

    fun getEglContext(): EGLContext? {
        if (eglThread != null) {
            return eglThread!!.getEglContext()
        }
        return null
    }

    fun getTextureID() : Int{
        return m_textureid
    }

    fun getAUOSurfaceTexture() : AUOSurfaceTexture?{
        return m_SurfaseTexture
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        auosurfaceCreated(holder)
    }

    fun auosurfaceCreated(holder: SurfaceHolder?) {
        if (surface == null) {
            surface = holder?.surface
        }
        if (surface == null) {
            return
        }
        val auoSurfaceView = this
        if(m_AUORender == null){
            val render = AUORender(m_context, m_textureid, m_isDeWarp)
            auoSurfaceView.setRender(render)
        }
        Log.d(m_tag,"surfaceCreated")
        val drawFrameCallback = object : AUORender.AUORenderCallback {
            override fun onSurfaceCreatedCallback() {
                if(m_SurfaseTexture == null) {
                    Log.d(m_tag,"m_SurfaceTexture is empty, it will be created automatically.")
                    m_textureid = m_AUORender!!.getTexture()
                    m_SurfaseTexture = AUOSurfaceTexture(m_textureid)
                    m_SurfaseTexture?.addListener { auoSurfaceView.requestRender() }
                    m_SurfaseTexture?.setDefaultBufferSize(m_displayWidth, m_displayHeight)
                    m_SurfaceOfTexture = Surface(m_SurfaseTexture)
                    m_SurfaseTexture_createdByThis = true
                    mTextureSize.width = m_displayWidth
                    mTextureSize.height = m_displayHeight
                }
                else{
                    Log.d(m_tag,"m_SurfaceTexture is from outside.")
                    m_SurfaseTexture?.addListener { auoSurfaceView.requestRender() }
                    mTextureSize.width = m_SurfaseTexture!!.getWidth()
                    mTextureSize.height = m_SurfaseTexture!!.getHeight()
                }
                m_AUORender!!.setTextureSize(mTextureSize)
                m_AUOGLSurfaceViewCallback?.onSurfaceCreatedCallback()
            }
        }
        m_AUORender?.setCustomRenderCallback(drawFrameCallback)


        eglThread = EGLThread(WeakReference(this))
        eglThread!!.isCreate = true
        eglThread!!.start()

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        eglThread!!.width = width
        eglThread!!.height = height
        eglThread!!.isChange = true
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        eglThread!!.onDestory()
        eglThread = null
        surface = null
        eglContext = null
    }

    // Override this method to handle touch events
    override fun onTouchEvent(event: MotionEvent): Boolean {

        val pointerCount = event.pointerCount
        val pointerProperties =
            arrayOfNulls<PointerProperties>(pointerCount)
        val pointerCoords = arrayOfNulls<PointerCoords>(pointerCount)
        for (i in 0 until pointerCount) {
            pointerProperties[i] = PointerProperties()
            pointerProperties[i]!!.id = event.getPointerId(i)
            pointerProperties[i]!!.toolType = event.getToolType(i)
            pointerCoords[i] = PointerCoords()
            pointerCoords[i]!!.x = event.getX(i) * (mTextureSize.cropWidth-1) / (this.width-1) + mTextureSize.offsetX
            pointerCoords[i]!!.y = event.getY(i) * (mTextureSize.cropHeight-1) / (this.height-1) + mTextureSize.offsetY
            pointerCoords[i]!!.pressure = event.getPressure(i)
            pointerCoords[i]!!.size = event.getSize(i)
        }

        val newevent : MotionEvent = MotionEvent.obtain(
            event.downTime, event.eventTime, event.action, event.pointerCount,
            pointerProperties,pointerCoords,event.metaState,event.buttonState,
            event.xPrecision,event.yPrecision,event.deviceId,event.edgeFlags,
            event.source,event.flags
        )

        m_AUOGLSurfaceViewCallback?.onTouchCallback(newevent)
        return true // Return true to indicate the event was handled
    }


    fun getSurface(): Surface? {
        return surface
    }

    fun getSurfceOfTexture(): Surface? {
        return m_SurfaceOfTexture
    }

    fun setAUOGLSurfaceViewCallback(callback: AUOGLSurfaceViewCallback) {
        m_AUOGLSurfaceViewCallback = callback
    }

    interface EGLRender {
        fun onSurfaceCreated()
        fun onSurfaceChanged(width: Int, height: Int)
        fun onDrawFrame()
    }

    internal class EGLThread(private var myGlSurfaceViewWeakReference: WeakReference<AUOGLSurfaceView>?) :
        Thread() {
        private var eglHelper: EGLHelper? = null
        private var isExit = false
        var isCreate: Boolean = false
        var isChange: Boolean = false
        private var isStart = false

        companion object {
            // Static-like function
            private val  m_ReadWriteLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
        }

        // Used to control manual refresh
        private var `object`: Any? = null
        var width: Int = 0
        var height: Int = 0
        override fun run() {
            super.run()
            isExit = false
            isStart = false
            `object` = Any()
            eglHelper = EGLHelper()
            eglHelper?.initEgl(
                myGlSurfaceViewWeakReference!!.get()!!.surface,
                myGlSurfaceViewWeakReference!!.get()!!.eglContext
            )
            onCreate()
            while (true) {
                if (isExit) {
                    // Release resources
                    release()
                    break
                }
                /**
                 * Refresh modes
                 */
                if (myGlSurfaceViewWeakReference!!.get()!!.renderMode == RenderMode.RENDERMODE_WHEN_DIRTY) {
                    synchronized(`object`!!) {
                        try {
                            (`object` as Object).wait((1000.0f / 60.0f).toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    onUpdateTexure()
                    onChange(width, height)
                    onDraw()
                    isStart = true
                } else if (myGlSurfaceViewWeakReference!!.get()!!.renderMode == RenderMode.RENDERMODE_CONTINUOUSLY) {
                    // Auto refresh, 60 frames per second
                    try {
                        sleep((1000 / 60).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    onUpdateTexure()
                    onChange(width, height)
                    onDraw()
                    isStart = true
                }
            }
        }

        /**
         * Create, execute once
         */
        private fun onCreate() {
            if (isCreate && myGlSurfaceViewWeakReference!!.get()!!.m_AUORender != null) {
                isCreate = false
                myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onSurfaceCreated()
            }
        }

        /**
         * Change, execute once
         *
         * @param width
         * @param height
         */
        private fun onChange(width: Int, height: Int) {
            if (isChange && myGlSurfaceViewWeakReference!!.get()!!.m_AUORender != null) {
                isChange = false
                myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onSurfaceChanged(width, height)
            }
        }


        private fun onUpdateTexure() {
            if (myGlSurfaceViewWeakReference!!.get()!!.m_SurfaseTexture_createdByThis) {
                m_ReadWriteLock.write {
                    myGlSurfaceViewWeakReference!!.get()!!.m_SurfaseTexture?.updateTexImage()
                }
            }
        }

        /**
         * Draw, execute every loop
         */
        private fun onDraw() {
            if (myGlSurfaceViewWeakReference!!.get()!!.m_AUORender != null && eglHelper != null) {
                m_ReadWriteLock.read {
                    myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onDrawFrame()
                    //The first time you refresh, you need to refresh twice.
                    if (!isStart) {
                        myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onDrawFrame()
                    }
                    eglHelper?.swapBuffers()
                }
            }
        }

        /**
         * Manual refresh
         * Release the blocking wait in the thread
         */
        internal fun requestRender() {
            if (`object` != null) {
                synchronized(`object`!!) {
                    (`object` as Object).notifyAll()
                    //Log.d("requestRender","notifyAll")
                }
            }
        }

        fun onDestory() {
            isExit = true
            requestRender()
        }

        fun release() {
            if (eglHelper != null) {
                eglHelper?.destoryEgl()
                eglHelper = null
                `object` = null
                myGlSurfaceViewWeakReference = null
            }
        }

        fun getEglContext(): EGLContext? {
            if (eglHelper != null) {
                return eglHelper?.getmEglContext()
            }
            return null
        }
    }

    companion object {
        private const val TAG = "MyGlSurfaceView"
    }
}


