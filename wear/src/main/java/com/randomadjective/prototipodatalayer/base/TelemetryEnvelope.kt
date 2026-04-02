package com.randomadjective.prototipodatalayer.base

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

    fun wrap(rawMessage: String, inputFamily: String, eventType: String): String {
        return JSONObject().apply {
            put("schema_version", 1)
            put("event_type", eventType) // input | sensor
            put("event_id", nextEventId())
            put("session_id", sessionId)

            put("input_family", inputFamily)
            put("raw_message", rawMessage)

            put("send_ts_watch_ns", SystemClock.elapsedRealtimeNanos())
            put("smartwatch_model", watchModel)

            put("receive_ts_phone_native_ns", 0)
            put("forward_ts_phone_native_ns", 0)
            put("receive_ts_unity_ns", 0)
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