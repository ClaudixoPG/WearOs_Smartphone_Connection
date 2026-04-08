package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object TelemetryEnvelope {

    private val counter = AtomicLong(0)
    private val sessionId = buildSessionId()
    private val watchModel = Build.MODEL ?: "UnknownWatch"

    private fun buildSessionId(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    private fun nextEventId(): String {
        return String.format(Locale.US, "W-%s-%06d", sessionId, counter.incrementAndGet())
    }

    fun wrap(
        context: Context,
        rawMessage: String,
        inputFamily: String,
        eventType: String
    ): String {

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevelWatch = if (level >= 0 && scale > 0) {
            level * 100.0 / scale
        } else -1.0

        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperatureWatchC = if (temp >= 0) temp / 10.0 else -1.0

        return JSONObject().apply {
            put("schema_version", 1)
            put("event_type", eventType)
            put("event_id", nextEventId())
            put("session_id", sessionId)

            put("input_family", inputFamily)
            put("raw_message", rawMessage)

            put("send_ts_watch_ns", SystemClock.elapsedRealtimeNanos())
            put("smartwatch_model", watchModel)

            put("battery_level_watch", batteryLevelWatch)
            put("temperature_watch_c", temperatureWatchC)

            put("receive_ts_phone_native_ns", 0)
            put("forward_ts_phone_native_ns", 0)
            put("receive_ts_unity_ns", 0)

            put("battery_level_phone", -1.0)
            put("temperature_phone_c", -1.0)
        }.toString()
    }

    fun isJson(message: String): Boolean {
        return try {
            JSONObject(message).has("schema_version")
        } catch (e: Exception) {
            false
        }
    }
}