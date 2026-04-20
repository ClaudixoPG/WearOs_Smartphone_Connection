package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WearTelemetryCsvLogger {

    private const val TAG = "Wear_CSV"

    private var eventsFile: File? = null
    private var sessionFile: File? = null
    private var eventsHeaderWritten = false
    private var sessionHeaderWritten = false

    private fun ensureFiles(context: Context) {
        if (eventsFile == null) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            eventsFile = File(context.filesDir, "watch_events_$timestamp.csv")
            if (!eventsFile!!.exists()) {
                eventsFile!!.createNewFile()
            }
        }

        if (sessionFile == null) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            sessionFile = File(context.filesDir, "watch_sessions_$timestamp.csv")
            if (!sessionFile!!.exists()) {
                sessionFile!!.createNewFile()
            }
        }

        if (!eventsHeaderWritten) {
            FileWriter(eventsFile, true).use { writer ->
                writer.appendLine(
                    listOf(
                        "event_id",
                        "session_id",
                        "minigame_id",
                        "input_family",
                        "raw_message",
                        "send_ts_watch_ns",
                        "ack_receive_ts_watch_ns",
                        "rtt_ms",
                        "one_way_est_ms",
                        "smartwatch_model",
                        "smartphone_model"
                    ).joinToString(",")
                )
            }
            eventsHeaderWritten = true
        }

        if (!sessionHeaderWritten) {
            FileWriter(sessionFile, true).use { writer ->
                writer.appendLine(
                    listOf(
                        "session_id",
                        "minigame_id",
                        "smartwatch_model",
                        "smartphone_model",
                        "battery_level_watch_start",
                        "battery_level_watch_end",
                        "temperature_watch_start",
                        "temperature_watch_end",
                        "battery_level_phone_start",
                        "battery_level_phone_end",
                        "temperature_phone_start",
                        "temperature_phone_end",
                        "start_timestamp_utc",
                        "end_timestamp_utc",
                        "event_count",
                        "ack_timeout_count"
                    ).joinToString(",")
                )
            }
            sessionHeaderWritten = true
        }

        Log.i(TAG, "Events CSV path: ${eventsFile?.absolutePath}")
        Log.i(TAG, "Session CSV path: ${sessionFile?.absolutePath}")
    }

    fun flushCompletedEvents(
        context: Context,
        events: List<WearSessionTelemetryStore.CompletedInputEvent>
    ) {
        if (events.isEmpty()) return

        try {
            ensureFiles(context)
            FileWriter(eventsFile, true).use { writer ->
                for (event in events) {
                    writer.appendLine(
                        listOf(
                            escape(event.eventId),
                            escape(event.sessionId),
                            escape(event.minigameId),
                            escape(event.inputFamily),
                            escape(event.rawMessage),
                            event.sendTsWatchNs.toString(),
                            event.ackReceiveTsWatchNs.toString(),
                            formatDouble(event.rttMs),
                            formatDouble(event.oneWayEstMs),
                            escape(event.watchModel),
                            escape(event.phoneModel)
                        ).joinToString(",")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush completed events", e)
        }
    }

    fun flushSessionSummary(
        context: Context,
        session: WearSessionTelemetryStore.MinigameSessionRecord
    ) {
        try {
            ensureFiles(context)
            FileWriter(sessionFile, true).use { writer ->
                writer.appendLine(
                    listOf(
                        escape(session.sessionId),
                        escape(session.minigameId),
                        escape(session.watchModel),
                        escape(session.phoneModel),
                        formatDouble(session.batteryLevelWatchStart),
                        formatDouble(session.batteryLevelWatchEnd),
                        formatDouble(session.temperatureWatchStart),
                        formatDouble(session.temperatureWatchEnd),
                        formatDouble(session.batteryLevelPhoneStart),
                        formatDouble(session.batteryLevelPhoneEnd),
                        formatDouble(session.temperaturePhoneStart),
                        formatDouble(session.temperaturePhoneEnd),
                        escape(session.startTimestampUtc),
                        escape(session.endTimestampUtc),
                        session.eventCount.toString(),
                        session.ackTimeoutCount.toString()
                    ).joinToString(",")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush session summary", e)
        }
    }

    private fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.4f", value)
    }

    private fun escape(value: String?): String {
        if (value.isNullOrEmpty()) return "\"\""
        return "\"${value.replace("\"", "\"\"")}\""
    }
}