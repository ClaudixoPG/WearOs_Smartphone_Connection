package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object WearMessageSender {

    private const val PATH = "/mensaje"
    private const val TAG = "Wear_Send"
    private const val NODE_CACHE_TTL_MS = 5000L

    // El executor sigue siendo single-threaded: solo sirve para serializar
    // la preparación del mensaje (clasificar, construir payload, leer caché).
    // El envío BT ya no bloquea este hilo.
    private val sendExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cachedNodeIds: List<String> = emptyList()

    @Volatile
    private var lastNodeRefreshMs: Long = 0L

    @Volatile
    private var currentMinigameId: String = "unknown_minigame"

    // Evita disparar múltiples refrescos de nodos simultáneos en background.
    @Volatile
    private var nodeRefreshInFlight: Boolean = false

    // Sampling counters (para escalabilidad futura)
    private val counters = HashMap<String, AtomicInteger>()

    fun warmup(context: Context) {
        val appContext = context.applicationContext
        sendExecutor.execute {
            try {
                refreshNodesAsync(appContext)
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

                // Camino crítico: lectura no bloqueante del caché de nodos.
                val nodeIds = getNodesOrEmpty()
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No nodes connected — triggering async refresh")
                    refreshNodesAsync(appContext)
                    return@execute
                }

                // Si el caché está cerca de vencer, refrescamos en background
                // proactivamente, sin bloquear este envío.
                maybeRefreshNodesInBackground(appContext)

                val bytes = payload.toByteArray(Charsets.UTF_8)

                // Fire-and-forget: NO Tasks.await(). El executor queda libre
                // inmediatamente para procesar el siguiente mensaje en cola.
                for (nodeId in nodeIds) {
                    Wearable.getMessageClient(appContext)
                        .sendMessage(nodeId, PATH, bytes)
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Send error to node=$nodeId", e)
                            invalidateNodeCache()
                            refreshNodesAsync(appContext)
                        }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Send prepare error", e)
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
    // Test Session (RUN)
    // -------------------------

    fun startTestSession(context: Context) {
        Log.i(TAG, "TEST SESSION START")

        WearSessionTelemetryStore.resetAll() // limpia estado anterior
        WearTelemetryCsvLogger.startNewRun(context)
    }

    fun endTestSession(context: Context) {
        Log.i(TAG, "TEST SESSION END")

        val events = WearSessionTelemetryStore.drainCompletedEvents()
        WearTelemetryCsvLogger.flushCompletedEvents(context, events)

        WearTelemetryCsvLogger.closeRun() // opcional pero recomendable
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

    // -------------------------
    // Node discovery (no bloqueante)
    // -------------------------

    /**
     * Lectura inmediata del caché. Nunca bloquea el camino de envío.
     * Si está vacío, el caller debe disparar refreshNodesAsync().
     */
    private fun getNodesOrEmpty(): List<String> = cachedNodeIds

    /**
     * Refresca el caché en background si está próximo a vencer.
     * No bloquea el executor thread.
     */
    private fun maybeRefreshNodesInBackground(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastNodeRefreshMs >= NODE_CACHE_TTL_MS) {
            refreshNodesAsync(context)
        }
    }

    /**
     * Refresco asíncrono usando el API de callbacks de Wearable.
     * No bloquea ningún hilo; el resultado se escribe al caché cuando llega.
     * Flag nodeRefreshInFlight previene refrescos simultáneos redundantes.
     */
    private fun refreshNodesAsync(context: Context) {
        if (nodeRefreshInFlight) return
        nodeRefreshInFlight = true

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                cachedNodeIds = nodes.map { it.id }
                lastNodeRefreshMs = System.currentTimeMillis()
                nodeRefreshInFlight = false
                Log.d(TAG, "Node cache refreshed: ${cachedNodeIds.size} node(s)")
            }
            .addOnFailureListener { e ->
                nodeRefreshInFlight = false
                Log.w(TAG, "Node refresh failed", e)
            }
    }

    private fun invalidateNodeCache() {
        cachedNodeIds = emptyList()
        lastNodeRefreshMs = 0L
    }
}