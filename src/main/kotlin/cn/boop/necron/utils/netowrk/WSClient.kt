package cn.boop.necron.utils.netowrk

import cn.boop.necron.Necron
import cn.boop.necron.utils.modMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.odtheking.odin.OdinMod.mc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicReference

object WSClient {
    const val PREFIX = "§bWS §8»§r "
    private val gson: Gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()

    private val wsRef = AtomicReference<WebSocket?>(null)
    private val httpClient = HttpClient.newHttpClient()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    var playerUUID: String = ""
    var playerIGN: String = ""

    val isConnected: Boolean get() = wsRef.get() != null

    var onBroadcast: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onEvent: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onPlayerJoin: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onPlayerLeave: ((WebSocketManager.ServerMessage) -> Unit)? = null

    fun initialize(uuid: String, ign: String) {
        playerUUID = uuid
        playerIGN = ign
    }

    fun connect() {
        if (isConnected) {
            Necron.logger.info("WS already connected.")
            return
        }

        val listener = WSListener()
        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(WSConfig.SERVER_URL), listener)
            .exceptionally { error ->
                Necron.logger.info("Connect failed: ${error.message}")
                scheduleReconnect()
                null
            }
    }

    fun disconnect() {
        stopHeartbeat()
        reconnectJob?.cancel()
        wsRef.getAndSet(null)?.sendClose(1000, "Client shutdown")?.join()
        Necron.logger.info("WS Disconnected.")
    }

    fun sendChat(content: String) {
        if (!isConnected) return
        val msg = WebSocketManager.ClientMessage.chat(playerUUID, playerIGN, content)
        send(gson.toJson(msg))
    }

    fun sendEvent(eventData: WebSocketManager.EventData): Boolean {
        if (!isConnected) return false
        val msg = WebSocketManager.ClientMessage.event(playerUUID, playerIGN, eventData)
        return send(gson.toJson(msg))
    }

    private fun send(payload: String): Boolean {
        return wsRef.get()?.let { ws ->
            ws.sendText(payload, true)
            true
        } ?: false
    }

    private fun sendConnect() {
        val msg = mc.currentServer?.ip?.let { WebSocketManager.ClientMessage.connect(playerUUID, playerIGN, it) }
        send(gson.toJson(msg))
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(WSConfig.HEARTBEAT_INTERVAL.toLong())
                wsRef.get()?.sendText(
                    gson.toJson(WebSocketManager.ClientMessage.ping(playerUUID, playerIGN)), true
                )
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(WSConfig.RECONNECT_DELAY.toLong())
            if (!isConnected) {
                Necron.logger.info("Reconnecting...")
                connect()
            }
        }
    }

    private class WSListener : WebSocket.Listener {
        private val buffer = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            Necron.logger.info("Connected to WebSocket server.")
            wsRef.set(webSocket)
            sendConnect()
            startHeartbeat()
            webSocket.request(1)
        }

        override fun onText(ws: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
            buffer.append(data)
            if (last) {
                handleRawMessage(buffer.toString())
                buffer.clear()
            }
            ws.request(1)
            return CompletableFuture.completedFuture(null)
        }

        override fun onBinary(ws: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> {
            val text = StandardCharsets.UTF_8.decode(data).toString()
            handleRawMessage(text)
            ws.request(1)
            return CompletableFuture.completedFuture(null)
        }

        override fun onClose(ws: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
            Necron.logger.info("Closed: $statusCode - $reason")
            wsRef.compareAndSet(ws, null)
            stopHeartbeat()
            if (mc.level != null) scheduleReconnect()
            return CompletableFuture.completedFuture(null)
        }

        override fun onError(ws: WebSocket, error: Throwable) {
            Necron.logger.error("Error: ${error.message}")
            wsRef.compareAndSet(ws, null)
            stopHeartbeat()
            if (mc.level != null) scheduleReconnect()
        }
    }

    private fun handleRawMessage(raw: String) {
        try {
            val msg = gson.fromJson(raw, WebSocketManager.ServerMessage::class.java)
            when (msg.type) {
                "broadcast" -> onBroadcast?.invoke(msg)
                "event" -> onEvent?.invoke(msg)
                "player_join" -> onPlayerJoin?.invoke(msg)
                "player_leave" -> onPlayerLeave?.invoke(msg)
                "error" -> modMessage("§4Server: ${msg.type}")
            }
        } catch (e: Exception) {
            Necron.logger.error("§4Parse error: ${e.message}")
        }
    }
}