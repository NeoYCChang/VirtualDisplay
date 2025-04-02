package com.yuchen.virtualdisplay.AUOScreenMirroring

import android.util.Log
import android.view.MotionEvent
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale


class AUOWebSocketServer : WebSocketServer {

    private val m_tag: String  = "AUOWebSocketServer"
    private val m_socketClients : ArrayList<WebSocket>  = ArrayList()

    constructor(socketAddress: InetSocketAddress) : super(socketAddress) {

    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
        Log.d(m_tag, "onOpen")
        m_socketClients.add(conn)
        //mWebSocket = conn
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String, remote: Boolean) {
        Log.d(m_tag, "onClose:$reason")
        m_socketClients.remove(conn)
    }

    override fun onMessage(conn: WebSocket?, message: String?) {}

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.d(m_tag, "onError:$ex")
    }

    override fun onStart() {
        Log.d(m_tag, "onStart")
    }

    fun sendData(bytes: ByteArray?) {
        m_socketClients.forEach { client ->
            if (client.isOpen) {
                client.send(bytes)

            }
        }
    }

    fun close() {
        m_socketClients.forEach { client ->
            if (client.isOpen) {
                // close each open WebSocket connection
                Log.d(m_tag, "close client")
                client.close()
            }
        }
        m_socketClients.clear()
    }


}