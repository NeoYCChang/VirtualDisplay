package com.yuchen.virtualdisplay.GLVirtualDisplay
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface

//https://blog.csdn.net/yu540135101/article/details/102880518
//https://blog.csdn.net/yu540135101/article/details/102938330
interface CustomGLSurfaceViewCallback {
    fun onSurfaceCreatedCallback()
}

class CustomGLSurfaceView(context: Context, textureid : Int, surfaseTexture: CustomSurfaceTexture?, width : Int, height : Int) : GLSurfaceView(context) {

    private var m_CustomGLSurfaceViewCallback: CustomGLSurfaceViewCallback? = null
    private var m_CustomRender: CustomRender? = null
    private var m_SurfaseTexture: CustomSurfaceTexture? = null
    private var m_Surface: Surface?  =  null

    companion object {
        // Static-like function
        var index: Int = 0
    }

    init {
        val glSurfaceView = this
        glSurfaceView.setEGLContextClientVersion(2)
        m_CustomRender = CustomRender(context, textureid)
        glSurfaceView.setRenderer(m_CustomRender)
        glSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY)

        val drawFrameCallback = object : CustomRenderCallback {
            override fun onSurfaceCreatedCallback() {
                if(surfaseTexture == null && m_SurfaseTexture == null) {
                    Log.d("test","debug1")
                    m_SurfaseTexture = CustomSurfaceTexture(m_CustomRender!!.getTexture())
                    m_SurfaseTexture?.addListener { glSurfaceView.requestRender() }
                    m_SurfaseTexture?.setDefaultBufferSize(width, height)
                    m_Surface = Surface(m_SurfaseTexture)
                }
                else{
                    Log.d("test","debug2")
                    m_SurfaseTexture = surfaseTexture
                    m_SurfaseTexture?.addListener { glSurfaceView.requestRender() }
                }
                m_CustomGLSurfaceViewCallback?.onSurfaceCreatedCallback()
            }
            override fun onDrawFrameCallback() {
                m_SurfaseTexture?.updateTexImage()
                Log.d("updateTexImage", "updateTexImage")
            }
        }
        m_CustomRender?.setCustomRenderCallback(drawFrameCallback)
    }

    fun getSurfaseTexture() : CustomSurfaceTexture?{
        return m_SurfaseTexture
    }

    fun getTextureId() : Int{
        if(m_CustomRender != null)
        {
            return m_CustomRender!!.getTexture()
        }
        else
        {
            return -1
        }
    }

    fun getSurface() : Surface?{
        return m_Surface
    }

    fun setCustomGLSurfaceViewCallback(callback: CustomGLSurfaceViewCallback) {
        m_CustomGLSurfaceViewCallback = callback
    }

}