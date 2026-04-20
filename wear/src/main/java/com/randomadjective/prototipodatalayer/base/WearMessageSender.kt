package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object WearMessageSender {

    private const val PATH = "/mensaje"
    private const val TAG = "Wear_Send"
    private const val NODE_CACHE_TTL_MS = 5000L

    private val sendExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cachedNodeIds: List<String> = emptyList()

    @Volatile
    private var lastNodeRefreshMs: Long = 0L

    @Volatile
    private var currentMinigameId: String = "unknown_minigame"

    // Sampling counters (para escalabilidad futura)
    private val counters = HashMap<String, AtomicInteger>()

    fun warmup(context: Context) {
        val appContext = context.applicationContext
        sendExecutor.execute {
            try {
                refreshNodesIfNeeded(appContext, true)
                Log.d(TAG, "Warmup OK")
            } catch (e: Exception) {
                Log.e(TAG, "Warmup error", e)
            }
        }
    }

    fun startMinigameSession(context: Context, minigameId: String) {
        currentMinigameId = minigameId
        WearSessionTelemetryStore.startMinigameSession(context, minigameId)
    }

    fun endMinigameSession(context: Context) {
        val events = WearSessionTelemetryStore.drainCompletedEvents()
        WearTelemetryCsvLogger.flushCompletedEvents(context, events)

        val session = WearSessionTelemetryStore.endMinigameSession(context, -1.0, -1.0)
        if (session != null) {
            WearTelemetryCsvLogger.flushSessionSummary(context, session)
        }
    }

    // 🔥 MÉTODO PRINCIPAL — GENÉRICO
    fun sendMessage(context: Context, rawMessage: String) {
        val appContext = context.applicationContext

        sendExecutor.execute {
            try {
                val inputFamily = classifyMessage(rawMessage)
                val shouldMeasure = shouldMeasure(inputFamily)

                val payload: String

                if (shouldMeasure) {
                    val pending = WearSessionTelemetryStore.createPendingEvent(
                        minigameId = currentMinigameId,
                        inputFamily = inputFamily,
                        rawMessage = rawMessage
                    )

                    payload = InputMessageCodec.buildMeasuredEvent(
                        pending.eventId,
                        pending.inputFamily,
                        pending.rawMessage
                    )
                } else {
                    payload = InputMessageCodec.buildRawEvent(
                        inputFamily,
                        rawMessage
                    )
                }

                val nodeIds = refreshNodesIfNeeded(appContext, false)
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No nodes connected")
                    return@execute
                }

                val bytes = payload.toByteArray(Charsets.UTF_8)

                for (nodeId in nodeIds) {
                    Tasks.await(
                        Wearable.getMessageClient(appContext)
                            .sendMessage(nodeId, PATH, bytes)
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Send error", e)
                invalidateNodeCache()
            }
        }
    }

    fun handleIncomingMessage(context: Context, message: String): Boolean {
        val ack = InputMessageCodec.parseAck(message) ?: return false

        WearSessionTelemetryStore.completeAck(
            ack.eventId,
            ack.phoneModel
        )

        return true
    }

    // -------------------------
    // Helpers
    // -------------------------

    private fun classifyMessage(message: String): String {
        return when {
            message.startsWith("Tap") -> "Tap"
            message.startsWith("Dpad") -> "Dpad"
            message.startsWith("Joystick") -> "Joystick"
            message.startsWith("Hold") -> "Hold"
            message.startsWith("Gyro") -> "Gyro"
            else -> "Unknown"
        }
    }

    private fun shouldMeasure(inputFamily: String): Boolean {
        return when (inputFamily) {
            "Tap", "Dpad" -> true
            "Hold" -> sample("Hold", 10)
            "Joystick" -> sample("Joystick", 10)
            "Gyro" -> sample("Gyro", 15)
            else -> false
        }
    }

    private fun sample(key: String, interval: Int): Boolean {
        val counter = counters.getOrPut(key) { AtomicInteger(0) }
        return counter.incrementAndGet() % interval == 0
    }

    private fun refreshNodesIfNeeded(context: Context, force: Boolean): List<String> {
        val now = System.currentTimeMillis()

        if (!force &&
            cachedNodeIds.isNotEmpty() &&
            now - lastNodeRefreshMs < NODE_CACHE_TTL_MS
        ) {
            return cachedNodeIds
        }

        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        cachedNodeIds = nodes.map { it.id }
        lastNodeRefreshMs = now

        return cachedNodeIds
    }

    private fun invalidateNodeCache() {
        cachedNodeIds = emptyList()
        lastNodeRefreshMs = 0L
    }
}