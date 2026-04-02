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
                // 🔹 Detectar tipo automáticamente
                val (inputFamily, eventType) = classifyMessage(message)

                // 🔹 Envolver con telemetría
                val payload = TelemetryEnvelope.wrap(
                    rawMessage = message,
                    inputFamily = inputFamily,
                    eventType = eventType
                )

                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                Log.i(TAG, "Nodos conectados: ${nodes.size}")

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No hay nodos conectados para enviar el mensaje.")
                }

                for (node in nodes) {
                    val task = Wearable.getMessageClient(context)
                        .sendMessage(node.id, PATH, payload.toByteArray())
                    Tasks.await(task)
                    Log.i(TAG, "Mensaje enviado a ${node.displayName}: $payload")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar mensaje: ${e.message}", e)
            }
        }.start()
    }

    private fun classifyMessage(message: String): Pair<String, String> {

        return when {
            message.startsWith("Tap") -> "Tap" to "input"

            message.startsWith("Joystick") ||
                    message.startsWith("JoystickRelease") -> "Joystick" to "input"

            message.startsWith("Dpad") ||
                    message.startsWith("DpadRelease") -> "Dpad" to "input"

            message.startsWith("fuerza") ||
                    message.startsWith("fuerzaRelease") -> "Force" to "input"

            message.startsWith("Gyro") -> "Gyroscope" to "sensor"

            message.startsWith("Location") -> "Location" to "sensor"

            message.startsWith("HeartRate") -> "HeartRate" to "sensor"

            else -> "Unknown" to "input"
        }
    }
}