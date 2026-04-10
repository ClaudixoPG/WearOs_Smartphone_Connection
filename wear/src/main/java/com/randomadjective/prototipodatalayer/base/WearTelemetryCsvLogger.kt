package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WearTelemetryCsvLogger {

    private const val TAG = "Wear_CSV"

    private var file: File? = null
    private var headerWritten = false

    private fun ensureFile(context: Context) {
        if (file != null) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "watch_latency_$timestamp.csv"
        file = File(context.filesDir, fileName)

        if (!file!!.exists()) {
            file!!.createNewFile()
        }

        if (!headerWritten) {
            FileWriter(file, true).use { writer ->
                writer.appendLine(
                    listOf(
                        "event_id",
                        "session_id",
                        "event_type",
                        "input_family",
                        "raw_message",
                        "send_ts_watch_ns",
                        "ack_receive_ts_watch_ns",
                        "rtt_watch_phone_ms",
                        "watch_to_phone_one_way_est_ms",
                        "smartwatch_model",
                        "battery_level_watch",
                        "temperature_watch_c",
                        "test_timestamp_utc"
                    ).joinToString(",")
                )
            }
            headerWritten = true
        }

        Log.i(TAG, "Watch CSV path: ${file?.absolutePath}")
    }

    fun logLatencyResult(
        context: Context,
        eventId: String,
        sessionId: String,
        eventType: String,
        inputFamily: String,
        rawMessage: String,
        sendTsWatchNs: Long,
        ackReceiveTsWatchNs: Long,
        rttMs: Double,
        oneWayEstMs: Double,
        batteryLevelWatch: Double,
        temperatureWatchC: Double
    ) {
        try {
            ensureFile(context)

            FileWriter(file, true).use { writer ->
                writer.appendLine(
                    listOf(
                        escape(eventId),
                        escape(sessionId),
                        escape(eventType),
                        escape(inputFamily),
                        escape(rawMessage),
                        sendTsWatchNs.toString(),
                        ackReceiveTsWatchNs.toString(),
                        formatDouble(rttMs),
                        formatDouble(oneWayEstMs),
                        escape(Build.MODEL ?: "UnknownWatch"),
                        formatDouble(batteryLevelWatch),
                        formatDouble(temperatureWatchC),
                        escape(
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.US
                            ).format(Date())
                        )
                    ).joinToString(",")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log watch latency result", e)
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