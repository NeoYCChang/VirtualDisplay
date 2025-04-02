package com.yuchen.virtualdisplay.AUOScreenMirroring

import android.media.MediaCodec
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUOGLSurfaceView
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUORender
import org.java_websocket.WebSocket
import java.net.InetSocketAddress
import java.util.concurrent.locks.ReentrantLock


class AUOMirrorManager {
    private val m_tag = "AUOMirrorManager"
    private val SOCKET_PORT: Int = 50000
    private var mAUOScreenEncoder: AUOScreenEncoder? = null
    private var mAUOWebSocketServer: AUOWebSocketServer? = null

    constructor(video_width : Int?, video_height: Int?){
        mAUOWebSocketServer = AUOWebSocketServer(InetSocketAddress(SOCKET_PORT))
        mAUOScreenEncoder = AUOScreenEncoder(this, video_width, video_height)
    }

    fun getSurface():Surface? {
        return mAUOScreenEncoder?.getSurface()
    }

    fun start() {
        mAUOWebSocketServer?.start()
        mAUOScreenEncoder?.startEncode()
    }

    fun close() {
        try {
            mAUOWebSocketServer?.stop()
            mAUOWebSocketServer?.close()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mAUOScreenEncoder?.stopEncode()
    }

    fun sendData(bytes: ByteArray?) {
        mAUOWebSocketServer?.sendData(bytes)
    }

}