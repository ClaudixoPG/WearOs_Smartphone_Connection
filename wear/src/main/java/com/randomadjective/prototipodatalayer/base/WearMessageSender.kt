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
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                Log.i(TAG, "Nodos conectados: ${nodes.size}")

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No hay nodos conectados para enviar el mensaje.")
                }

                for (node in nodes) {
                    val task = Wearable.getMessageClient(context)
                        .sendMessage(node.id, PATH, message.toByteArray())
                    Tasks.await(task)
                    Log.i(TAG, "Mensaje enviado a ${node.displayName}: $message")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al enviar mensaje: ${e.message}", e)
            }
        }.start()
    }
}