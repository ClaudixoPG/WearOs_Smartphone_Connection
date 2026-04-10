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
import java.util.concurrent.atomic.AtomicInteger

object WearMessageSender {

    private const val PATH = "/mensaje"
    private const val TAG = "Wear_Send"

    private const val CONTINUOUS_SAMPLE_EVERY = 10
    private const val ACK_TIMEOUT_MS = 2000L

    private val continuousCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val pendingEvents = ConcurrentHashMap<String, PendingEvent>()

    data class PendingEvent(
        val eventId: String,
        val sessionId: String,
        val eventType: String,
        val inputFamily: String,
        val rawMessage: String,
        val sendTsWatchNs: Long,
        val createdAtMs: Long
    )

    fun sendMessage(context: Context, message: String) {
        Thread {
            try {
                cleanupExpiredPending()

                val (inputFamily, eventType) = classifyMessage(message)
                val latencySampled = shouldSampleLatency(inputFamily)

                val payload = TelemetryEnvelope.wrap(
                    context = context,
                    rawMessage = message,
                    inputFamily = inputFamily,
                    eventType = eventType,
                    latencySampled = latencySampled
                )

                val json = JSONObject(payload)

                if (latencySampled) {
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

                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)

                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, PATH, payload.toByteArray())
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error enviando mensaje: ${e.message}", e)
            }
        }.start()
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
                        Log.w(TAG, "ACK recibido sin pending event: $eventId")
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

                        Log.i(
                            TAG,
                            "ACK procesado. eventId=$eventId rttMs=$rttMs oneWayEstMs=$oneWayEstMs"
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

    private fun shouldSampleLatency(inputFamily: String): Boolean {
        return if (isContinuousFamily(inputFamily)) {
            val counter = continuousCounters.getOrPut(inputFamily) { AtomicInteger(0) }
            val value = counter.incrementAndGet()
            value % CONTINUOUS_SAMPLE_EVERY == 0
        } else {
            true
        }
    }

    private fun isContinuousFamily(inputFamily: String): Boolean {
        return when (inputFamily) {
            "Joystick", "Gyroscope", "Location", "HeartRate", "Force" -> true
            else -> false
        }
    }

    private fun cleanupExpiredPending() {
        val now = System.currentTimeMillis()
        val iterator = pendingEvents.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAtMs > ACK_TIMEOUT_MS) {
                Log.w(TAG, "Pending RTT event expired: ${entry.key}")
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
            message.startsWith("fuerza") -> "Force" to "input"
            message.startsWith("Gyro") -> "Gyroscope" to "sensor"
            message.startsWith("Location") -> "Location" to "sensor"
            message.startsWith("HeartRate") -> "HeartRate" to "sensor"
            else -> "Unknown" to "input"
        }
    }
}