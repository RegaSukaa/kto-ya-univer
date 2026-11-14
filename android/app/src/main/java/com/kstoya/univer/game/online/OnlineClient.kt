package com.kstoya.univer.game.online

import android.util.Log
import com.kstoya.univer.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnStatus { IDLE, CONNECTING, CONNECTED, FAILED, CLOSED }

class OnlineClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    var onMessage: ((JSONObject) -> Unit)? = null
    var onStatus: ((ConnStatus, String?) -> Unit)? = null

    fun connect(baseUrl: String) {
        val url = normalize(baseUrl)
        onStatus?.invoke(ConnStatus.CONNECTING, null)
        val req = Request.Builder().url(url).build()
        socket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatus?.invoke(ConnStatus.CONNECTED, null)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = runCatching { JSONObject(text) }.getOrNull() ?: return
                onMessage?.invoke(obj)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("OnlineClient", "ws failure: ${t.message}")
                onStatus?.invoke(ConnStatus.FAILED, t.message)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatus?.invoke(ConnStatus.CLOSED, reason)
            }
        })
    }

    fun send(obj: JSONObject) {
        socket?.send(obj.toString())
    }

    fun close() {
        runCatching { socket?.close(1000, "bye") }
        socket = null
    }

    /** http(s)://host:port или host:port → ws(s)://host:port/ws */
    fun normalize(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) s = BuildConfig.SERVER_URL
        s = when {
            s.startsWith("wss://") || s.startsWith("ws://") -> s
            s.startsWith("https://") -> "wss://" + s.removePrefix("https://")
            s.startsWith("http://") -> "ws://" + s.removePrefix("http://")
            else -> "ws://$s"
        }
        if (!s.contains("/ws")) s = s.trimEnd('/') + "/ws"
        return s
    }
}
