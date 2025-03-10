package com.yuchen.virtualdisplay.AUOGLSurfaceView

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.yuchen.virtualdisplay.GLVirtualDisplay.CustomGLSurfaceViewCallback
import com.yuchen.virtualdisplay.GLVirtualDisplay.CustomRender
import com.yuchen.virtualdisplay.GLVirtualDisplay.CustomRenderCallback
import com.yuchen.virtualdisplay.GLVirtualDisplay.CustomSurfaceTexture
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext



class AUOGLSurfaceView @JvmOverloads constructor(
    context: Context ,textureid : Int, surfaseTexture: AUOSurfaceTexture?, width : Int, height : Int
) :
    SurfaceView(context), SurfaceHolder.Callback {

    interface AUOGLSurfaceViewCallback {
        fun onSurfaceCreatedCallback()
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
    private val m_width = width
    private val m_height = height
    private val m_context: Context = context
    private var m_SurfaceOfTexture: Surface? = null
    private var m_AUORender: AUORender?  = null
    private val m_tag = "AUOGLSurfaceView"
    private var m_SurfaseTexture_createdByThis = false
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

    fun requestRender() {
        if (eglThread != null) {
            Log.d(m_tag,"requestRender")
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
        if (surface == null) {
            surface = holder.surface
        }
        val auoSurfaceView = this
        if(m_AUORender == null){
            val render = AUORender(m_context, m_textureid)
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
                    m_SurfaseTexture?.setDefaultBufferSize(width, height)
                    m_SurfaceOfTexture = Surface(m_SurfaseTexture)
                    m_SurfaseTexture_createdByThis = true
                }
                else{
                    Log.d(m_tag,"m_SurfaceTexture is from outside.")
                    m_SurfaseTexture?.addListener { auoSurfaceView.requestRender() }
                }
                m_AUOGLSurfaceViewCallback?.onSurfaceCreatedCallback()
            }
            override fun onDrawFrameCallback() {
                if(m_SurfaseTexture_createdByThis) {
                    m_SurfaseTexture?.updateTexImage()
                    Log.d(m_tag, "onDrawFrameCallback")
                }
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
        private var m_need_to_render_queue: Boolean = false

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
                //if (isStart) {
                    if (myGlSurfaceViewWeakReference!!.get()!!.renderMode == RenderMode.RENDERMODE_WHEN_DIRTY) {
                        if(m_need_to_render_queue)
                        {
                            m_need_to_render_queue = false
                            onChange(width, height)
                            onDraw()
                            isStart = true
                        }
                        else
                        {
                            try {
                                sleep((1000 / 60).toLong())
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }

//                        synchronized(`object`!!) {
//                            try {
//                                (`object` as Object).wait()
//                            } catch (e: InterruptedException) {
//                                e.printStackTrace()
//                            }
//                        }
                    } else if (myGlSurfaceViewWeakReference!!.get()!!.renderMode == RenderMode.RENDERMODE_CONTINUOUSLY) {
                        // Auto refresh, 60 frames per second
                        try {
                            sleep((1000 / 60).toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        onChange(width, height)
                        onDraw()
                        isStart = true
                    }
                //}
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

        /**
         * Draw, execute every loop
         */
        private fun onDraw() {
            if (myGlSurfaceViewWeakReference!!.get()!!.m_AUORender != null && eglHelper != null) {
                myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onDrawFrame()
                //第一次刷新的时候，需要刷新两次
                if (!isStart) {
                    myGlSurfaceViewWeakReference!!.get()!!.m_AUORender!!.onDrawFrame()
                }
                eglHelper?.swapBuffers()
            }
        }

        /**
         * Manual refresh
         * Release the blocking wait in the thread
         */
        internal fun requestRender() {
            if (`object` != null) {
                m_need_to_render_queue = true
//                synchronized(`object`!!) {
//                    myGlSurfaceViewWeakReference!!.get()!!.m_SurfaseTexture?.updateTexImage()
//                    (`object` as Object).notifyAll()
//                    Log.d("requestRender","notifyAll")
//                }
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


