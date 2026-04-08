package com.randomadjective.prototipodatalayer.base

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

object WearMessageSender {

    private const val PATH = "/mensaje"
    private const val TAG = "Wear_Send"

    fun sendMessage(context: Context, message: String) {
        Thread {
            try {

                val (inputFamily, eventType) = classifyMessage(message)

                val payload = TelemetryEnvelope.wrap(
                    context,
                    message,
                    inputFamily,
                    eventType
                )

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