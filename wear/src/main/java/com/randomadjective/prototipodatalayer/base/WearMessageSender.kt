package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object WearMessageSender {

    private const val PATH = "/mensaje"
    private const val TAG = "Wear_Send"

    private const val HOLD_SAMPLE_EVERY = 10
    private const val JOYSTICK_SAMPLE_EVERY = 10
    private const val GYRO_SAMPLE_EVERY = 15
    private const val LOCATION_SAMPLE_EVERY = 15
    private const val HEART_RATE_SAMPLE_EVERY = 15

    private const val ACK_TIMEOUT_MS = 2000L
    private const val NODE_CACHE_TTL_MS = 5000L

    private val counters = ConcurrentHashMap<String, AtomicInteger>()
    private val pendingEvents = ConcurrentHashMap<String, PendingEvent>()

    private val sendExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cachedNodeIds: List<String> = emptyList()

    @Volatile
    private var lastNodeRefreshMs: Long = 0L

    data class PendingEvent(
        val eventId: String,
        val sessionId: String,
        val eventType: String,
        val inputFamily: String,
        val rawMessage: String,
        val sendTsWatchNs: Long,
        val createdAtMs: Long
    )

    /**
     * Llamar al entrar a la app o al reanudar, para evitar el primer lookup al momento del input.
     */
    fun warmup(context: Context) {
        val appContext = context.applicationContext
        sendExecutor.execute {
            try {
                refreshNodesIfNeeded(appContext, force = true)
                Log.d(TAG, "Warmup complete. Nodes=${cachedNodeIds.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Warmup failed", e)
            }
        }
    }

    /**
     * Ruta rápida para gameplay:
     * sin telemetry envelope, sin ack, sin JSON.
     */
    fun sendRawMessage(context: Context, message: String) {
        val appContext = context.applicationContext

        sendExecutor.execute {
            try {
                val nodeIds = refreshNodesIfNeeded(appContext, force = false)
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No connected nodes for RAW message: $message")
                    return@execute
                }

                val bytes = message.toByteArray(Charsets.UTF_8)

                for (nodeId in nodeIds) {
                    Tasks.await(
                        Wearable.getMessageClient(appContext)
                            .sendMessage(nodeId, PATH, bytes)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando mensaje RAW", e)

                // Si falló, intenta refrescar nodos para el próximo envío
                invalidateNodeCache()
            }
        }
    }

    /**
     * Ruta instrumentada original.
     * Déjala para pruebas de latencia o sensores cuando quieras usarla.
     */
    fun sendMessage(context: Context, message: String) {
        val appContext = context.applicationContext

        sendExecutor.execute {
            try {
                cleanupExpiredPending()

                val (inputFamily, eventType) = classifyMessage(message)
                val latencySampled = shouldSampleLatency(inputFamily)

                val payload = TelemetryEnvelope.wrap(
                    context = appContext,
                    rawMessage = message,
                    inputFamily = inputFamily,
                    eventType = eventType,
                    latencySampled = latencySampled
                )

                if (latencySampled) {
                    val json = JSONObject(payload)

                    val pending = PendingEvent(
                        eventId = json.optString("event_id", ""),
                        sessionId = json.optString("session_id", ""),
                        eventType = json.optString("event_type", ""),
                        inputFamily = json.optString("input_family", ""),
                        rawMessage = json.optString("raw_message", ""),
                        sendTsWatchNs = json.optLong("send_ts_watch_ns", 0L),
                        createdAtMs = System.currentTimeMillis()
                    )

                    if (pending.eventId.isNotBlank()) {
                        pendingEvents[pending.eventId] = pending
                    }
                }

                val nodeIds = refreshNodesIfNeeded(appContext, force = false)
                if (nodeIds.isEmpty()) {
                    Log.w(TAG, "No connected nodes for instrumented message")
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
                Log.e(TAG, "Error enviando mensaje instrumentado", e)
                invalidateNodeCache()
            }
        }
    }

    fun handleIncomingAck(context: Context, message: String): Boolean {
        return try {
            if (!TelemetryEnvelope.isJson(message)) {
                false
            } else {
                val json = JSONObject(message)
                val recordType = json.optString("record_type", "")

                if (recordType != "input_ack") {
                    false
                } else {
                    val eventId = json.optString("event_id", "")
                    val pending = pendingEvents.remove(eventId)

                    if (pending == null) {
                        true
                    } else {
                        val ackReceiveTsWatchNs = SystemClock.elapsedRealtimeNanos()
                        val rttMs = (ackReceiveTsWatchNs - pending.sendTsWatchNs) / 1_000_000.0
                        val oneWayEstMs = rttMs / 2.0

                        val (batteryLevelWatch, temperatureWatchC) = readWatchBattery(context)

                        WearTelemetryCsvLogger.logLatencyResult(
                            context = context,
                            eventId = pending.eventId,
                            sessionId = pending.sessionId,
                            eventType = pending.eventType,
                            inputFamily = pending.inputFamily,
                            rawMessage = pending.rawMessage,
                            sendTsWatchNs = pending.sendTsWatchNs,
                            ackReceiveTsWatchNs = ackReceiveTsWatchNs,
                            rttMs = rttMs,
                            oneWayEstMs = oneWayEstMs,
                            batteryLevelWatch = batteryLevelWatch,
                            temperatureWatchC = temperatureWatchC
                        )
                        true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleIncomingAck failed", e)
            false
        }
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

    private fun shouldSampleLatency(inputFamily: String): Boolean {
        return when (inputFamily) {
            "Tap", "Dpad" -> true
            "Hold" -> sampleEvery("Hold", HOLD_SAMPLE_EVERY)
            "Joystick" -> sampleEvery("Joystick", JOYSTICK_SAMPLE_EVERY)
            "Gyroscope" -> sampleEvery("Gyroscope", GYRO_SAMPLE_EVERY)
            "Location" -> sampleEvery("Location", LOCATION_SAMPLE_EVERY)
            "HeartRate" -> sampleEvery("HeartRate", HEART_RATE_SAMPLE_EVERY)
            else -> false
        }
    }

    private fun sampleEvery(key: String, interval: Int): Boolean {
        val counter = counters.getOrPut(key) { AtomicInteger(0) }
        val value = counter.incrementAndGet()
        return value % interval == 0
    }

    private fun cleanupExpiredPending() {
        val now = System.currentTimeMillis()
        val iterator = pendingEvents.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAtMs > ACK_TIMEOUT_MS) {
                iterator.remove()
            }
        }
    }

    private fun readWatchBattery(context: Context): Pair<Double, Double> {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevelWatch = if (level >= 0 && scale > 0) {
            level * 100.0 / scale
        } else {
            -1.0
        }

        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperatureWatchC = if (temp >= 0) temp / 10.0 else -1.0

        return batteryLevelWatch to temperatureWatchC
    }

    private fun classifyMessage(message: String): Pair<String, String> {
        return when {
            message.startsWith("Tap") -> "Tap" to "input"
            message.startsWith("Joystick") -> "Joystick" to "input"
            message.startsWith("Dpad") -> "Dpad" to "input"
            message.startsWith("Hold:") || message.startsWith("Time:") -> "Hold" to "input"
            message.startsWith("Gyro") -> "Gyroscope" to "sensor"
            message.startsWith("Location") -> "Location" to "sensor"
            message.startsWith("HeartRate") -> "HeartRate" to "sensor"
            else -> "Unknown" to "input"
        }
    }
}