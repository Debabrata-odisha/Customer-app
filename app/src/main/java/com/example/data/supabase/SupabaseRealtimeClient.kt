package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SupabaseRealtimeClient(
    private val config: SupabaseConfig
) {
    private val tag = "DoorblyRealtime"
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private val _realtimeEvents = MutableSharedFlow<RealtimeBookingUpdate>(extraBufferCapacity = 64)
    val realtimeEvents: SharedFlow<RealtimeBookingUpdate> = _realtimeEvents.asSharedFlow()

    data class RealtimeBookingUpdate(
        val eventType: String, // INSERT, UPDATE, DELETE
        val bookingId: String?,
        val newStatus: String?,
        val newPaymentStatus: String?
    )

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val projectUrl = config.projectUrl
        val anonKey = config.anonKey
        if (projectUrl.isEmpty() || anonKey.isEmpty()) {
            Log.d(tag, "Supabase configuration missing; skipping realtime connection")
            return
        }

        disconnect()

        val wsHost = projectUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        val wsUrl = "$wsHost/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Realtime WebSocket opened successfully")
                joinBookingsChannel(webSocket)
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Realtime closing: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "Realtime connection failure: ${t.message}")
            }
        })
    }

    private fun joinBookingsChannel(ws: WebSocket) {
        try {
            val joinPayload = JSONObject().apply {
                val configObj = JSONObject().apply {
                    val changesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "bookings")
                        })
                    }
                    put("postgres_changes", changesArray)
                }
                put("config", configObj)
            }

            val msg = JSONObject().apply {
                put("topic", "realtime:public:bookings")
                put("event", "phx_join")
                put("payload", joinPayload)
                put("ref", refCounter.getAndIncrement().toString())
            }

            ws.send(msg.toString())
            Log.d(tag, "Sent join channel for public:bookings")
        } catch (e: Exception) {
            Log.e(tag, "Error joining channel", e)
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25000)
                try {
                    val heartbeat = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", refCounter.getAndIncrement().toString())
                    }
                    ws.send(heartbeat.toString())
                } catch (e: Exception) {
                    Log.w(tag, "Heartbeat failed", e)
                    break
                }
            }
        }
    }

    private fun handleMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            val event = json.optString("event")
            if (event == "postgres_changes") {
                val payload = json.optJSONObject("payload")
                val data = payload?.optJSONObject("data")
                val type = data?.optString("type") ?: "UPDATE"
                val record = data?.optJSONObject("record")
                val bookingId = record?.optString("id")
                val status = record?.optString("status")
                val paymentStatus = record?.optString("payment_status")

                scope.launch {
                    _realtimeEvents.emit(
                        RealtimeBookingUpdate(
                            eventType = type,
                            bookingId = bookingId,
                            newStatus = status,
                            newPaymentStatus = paymentStatus
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse realtime message", e)
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            webSocket?.close(1000, "App closed")
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
    }
}
