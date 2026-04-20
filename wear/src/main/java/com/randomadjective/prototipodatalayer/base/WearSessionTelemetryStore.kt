package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

object WearSessionTelemetryStore {

    data class PendingInputEvent(
        val eventId: String,
        val sessionId: String,
        val minigameId: String,
        val inputFamily: String,
        val rawMessage: String,
        val sendTsWatchNs: Long,
        val createdAtMs: Long,
        val watchModel: String
    )

    data class CompletedInputEvent(
        val eventId: String,
        val sessionId: String,
        val minigameId: String,
        val inputFamily: String,
        val rawMessage: String,
        val sendTsWatchNs: Long,
        val ackReceiveTsWatchNs: Long,
        val rttMs: Double,
        val oneWayEstMs: Double,
        val watchModel: String,
        val phoneModel: String
    )

    data class MinigameSessionRecord(
        val sessionId: String,
        val minigameId: String,
        val watchModel: String,
        var phoneModel: String = "UnknownPhone",
        var batteryLevelWatchStart: Double = -1.0,
        var batteryLevelWatchEnd: Double = -1.0,
        var temperatureWatchStart: Double = -1.0,
        var temperatureWatchEnd: Double = -1.0,
        var batteryLevelPhoneStart: Double = -1.0,
        var batteryLevelPhoneEnd: Double = -1.0,
        var temperaturePhoneStart: Double = -1.0,
        var temperaturePhoneEnd: Double = -1.0,
        val startTimestampUtc: String,
        var endTimestampUtc: String = "",
        var eventCount: Int = 0,
        var ackTimeoutCount: Int = 0
    )

    private const val ACK_TIMEOUT_MS = 2000L

    private val watchModel: String = Build.MODEL ?: "UnknownWatch"
    private val sessionId: String = buildSessionId()
    private val eventCounter = AtomicLong(0)

    private val pendingEvents = ConcurrentHashMap<String, PendingInputEvent>()
    private val completedEvents = ConcurrentLinkedQueue<CompletedInputEvent>()

    @Volatile
    private var currentMinigameSession: MinigameSessionRecord? = null

    fun getSessionId(): String = sessionId

    fun getWatchModel(): String = watchModel

    fun nextEventId(): String {
        return String.format(Locale.US, "W-%s-%06d", sessionId, eventCounter.incrementAndGet())
    }

    fun startMinigameSession(context: Context, minigameId: String) {
        val (battery, temp) = readWatchBattery(context)

        currentMinigameSession = MinigameSessionRecord(
            sessionId = sessionId,
            minigameId = minigameId,
            watchModel = watchModel,
            batteryLevelWatchStart = battery,
            temperatureWatchStart = temp,
            startTimestampUtc = nowUtcString()
        )
    }

    fun setPhoneSessionStartInfo(
        phoneModel: String,
        batteryLevelPhone: Double,
        temperaturePhone: Double
    ) {
        currentMinigameSession?.let {
            if (phoneModel.isNotBlank()) it.phoneModel = phoneModel
            it.batteryLevelPhoneStart = batteryLevelPhone
            it.temperaturePhoneStart = temperaturePhone
        }
    }

    fun createPendingEvent(
        minigameId: String,
        inputFamily: String,
        rawMessage: String
    ): PendingInputEvent {
        cleanupExpiredPending()

        val event = PendingInputEvent(
            eventId = nextEventId(),
            sessionId = sessionId,
            minigameId = minigameId,
            inputFamily = inputFamily,
            rawMessage = rawMessage,
            sendTsWatchNs = SystemClock.elapsedRealtimeNanos(),
            createdAtMs = System.currentTimeMillis(),
            watchModel = watchModel
        )

        pendingEvents[event.eventId] = event
        return event
    }

    fun completeAck(eventId: String, phoneModel: String): CompletedInputEvent? {
        val pending = pendingEvents.remove(eventId) ?: return null
        val ackReceiveTsWatchNs = SystemClock.elapsedRealtimeNanos()
        val rttMs = (ackReceiveTsWatchNs - pending.sendTsWatchNs) / 1_000_000.0
        val oneWayEstMs = rttMs / 2.0

        val completed = CompletedInputEvent(
            eventId = pending.eventId,
            sessionId = pending.sessionId,
            minigameId = pending.minigameId,
            inputFamily = pending.inputFamily,
            rawMessage = pending.rawMessage,
            sendTsWatchNs = pending.sendTsWatchNs,
            ackReceiveTsWatchNs = ackReceiveTsWatchNs,
            rttMs = rttMs,
            oneWayEstMs = oneWayEstMs,
            watchModel = pending.watchModel,
            phoneModel = phoneModel.ifBlank { "UnknownPhone" }
        )

        completedEvents.add(completed)
        currentMinigameSession?.eventCount = (currentMinigameSession?.eventCount ?: 0) + 1
        currentMinigameSession?.phoneModel = completed.phoneModel
        return completed
    }

    fun endMinigameSession(
        context: Context,
        batteryLevelPhoneEnd: Double,
        temperaturePhoneEnd: Double
    ): MinigameSessionRecord? {
        val session = currentMinigameSession ?: return null
        val (battery, temp) = readWatchBattery(context)

        session.batteryLevelWatchEnd = battery
        session.temperatureWatchEnd = temp
        session.batteryLevelPhoneEnd = batteryLevelPhoneEnd
        session.temperaturePhoneEnd = temperaturePhoneEnd
        session.endTimestampUtc = nowUtcString()

        currentMinigameSession = null
        return session
    }

    fun drainCompletedEvents(): List<CompletedInputEvent> {
        val out = mutableListOf<CompletedInputEvent>()
        while (true) {
            val item = completedEvents.poll() ?: break
            out.add(item)
        }
        return out
    }

    private fun cleanupExpiredPending() {
        val now = System.currentTimeMillis()
        val iterator = pendingEvents.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAtMs > ACK_TIMEOUT_MS) {
                iterator.remove()
                currentMinigameSession?.ackTimeoutCount =
                    (currentMinigameSession?.ackTimeoutCount ?: 0) + 1
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

    private fun buildSessionId(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    private fun nowUtcString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}