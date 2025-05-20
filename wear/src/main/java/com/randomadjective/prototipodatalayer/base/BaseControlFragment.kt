package com.randomadjective.prototipodatalayer.base

import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

abstract class BaseControlFragment(layoutId: Int) : Fragment(layoutId) {

    protected val path = "/mensaje"

    /*protected fun enviarMensaje(mensaje: String) {
        Thread {
            val nodes = Tasks.await(Wearable.getNodeClient(requireContext()).connectedNodes)
            for (node in nodes) {
                Wearable.getMessageClient(requireContext())
                    .sendMessage(node.id, path, mensaje.toByteArray())
            }
        }.start()
    }*/

    protected fun enviarMensaje(mensaje: String) {
        Thread {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(requireContext()).connectedNodes)
                Log.i("Wear_Send", "Nodos conectados: ${nodes.size}")
                if (nodes.isEmpty()) {
                    Log.w("Wear_Send", "No hay nodos conectados para enviar el mensaje.")
                }
                for (node in nodes) {
                    val task = Wearable.getMessageClient(requireContext())
                        .sendMessage(node.id, path, mensaje.toByteArray())
                    Tasks.await(task)
                    Log.i("Wear_Send", "Mensaje enviado a ${node.displayName}: $mensaje")
                }
            } catch (e: Exception) {
                Log.e("Wear_Send", "Error al enviar mensaje: ${e.message}", e)
            }
        }.start()
    }
}
