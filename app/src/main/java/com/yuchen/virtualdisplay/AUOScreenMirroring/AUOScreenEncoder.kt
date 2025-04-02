package com.yuchen.virtualdisplay.AUOScreenMirroring

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class AUOScreenEncoder: Thread{

    private val SCREEN_FRAME_RATE: Int = 20
    private val SCREEN_FRAME_INTERVAL: Int = 1
    private val SOCKET_TIME_OUT: Long = 10000
    // I帧
    private val TYPE_FRAME_INTERVAL: Int = 19
    // vps帧
    private val TYPE_FRAME_VPS: Int = 32

    private var m_video_width: Int = 960
    private var m_video_height: Int = 540
    private var mMirrorManager: AUOMirrorManager? = null
    private var mMediaCodec: MediaCodec? = null
    private var m_playing = true
    private var m_surface : Surface? = null

    // 记录vps pps sps
    private var vps_pps_sps: ByteArray? = null

    private val m_tag: String  = "AUOScreenEncoder"

    constructor(mirrorManager: AUOMirrorManager, video_width : Int?, video_height: Int?) : super() {
        mMirrorManager =  mirrorManager
        if (video_width != null) {
            m_video_width = video_width
        }
        if (video_height != null) {
            m_video_height = video_height
        }

        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, m_video_width, m_video_height).apply {
            // Set the color format to Surface format
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            // Set the bitrate (bits per second)
            setInteger(MediaFormat.KEY_BIT_RATE, m_video_width * m_video_height * 2)
            // Set the frame rate
            setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
            // Set I-frame interval
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        }

        try {
            // Create the MediaCodec encoder
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            mMediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            // Create the input surface for encoding
            m_surface = mMediaCodec?.createInputSurface()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun startEncode() {
        start()
    }

    fun getSurface():Surface? {
        return m_surface
    }

    override fun run(){
        mMediaCodec!!.start()
        val bufferInfo = MediaCodec.BufferInfo()
        m_playing = true
        while (m_playing) {
            val outPutBufferId = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, SOCKET_TIME_OUT)
            if (outPutBufferId >= 0) {
                val byteBuffer = mMediaCodec!!.getOutputBuffer(outPutBufferId)
                if (byteBuffer != null) {
                    encodeData(byteBuffer, bufferInfo)
                }
                mMediaCodec!!.releaseOutputBuffer(outPutBufferId, false)
            }
        }
    }

    private fun encodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if(bufferInfo.size > 0) {
            var offSet = 4
            if (byteBuffer[2].toInt() == 0x01) {
                offSet = 3
            }
            val type = (byteBuffer[offSet].toInt() and 0x7E) shr 1
            if (type == TYPE_FRAME_VPS) {
                vps_pps_sps = ByteArray(bufferInfo.size)
                byteBuffer.get(vps_pps_sps)
            } else if (type == TYPE_FRAME_INTERVAL) {
                val bytes = ByteArray(bufferInfo.size)
                byteBuffer.get(bytes)

                var newBytes: ByteArray? = null
                if (vps_pps_sps != null) {
                    newBytes = ByteArray(vps_pps_sps!!.size + bytes.size)
                    System.arraycopy(vps_pps_sps, 0, newBytes, 0, vps_pps_sps!!.size)
                    System.arraycopy(bytes, 0, newBytes, vps_pps_sps!!.size, bytes.size)
                    mMirrorManager?.sendData(newBytes)
                } else {
                    newBytes = ByteArray(bytes.size)
                    System.arraycopy(bytes, 0, newBytes, 0, bytes.size)
                    mMirrorManager?.sendData(newBytes)
                }

            } else {
                val bytes = ByteArray(bufferInfo.size)
                byteBuffer[bytes]
                mMirrorManager?.sendData(bytes)
            }
        }
    }

    fun stopEncode() {
        m_playing = false
        if (mMediaCodec != null) {
            mMediaCodec!!.release()
        }
    }

    fun get_vps_pps_sps(): ByteArray?{
        return vps_pps_sps
    }

}