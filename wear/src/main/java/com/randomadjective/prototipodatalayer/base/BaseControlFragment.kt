package com.randomadjective.prototipodatalayer.base

import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

abstract class BaseControlFragment(layoutId: Int) : Fragment(layoutId) {

    protected val path = "/mensaje"

    protected fun sendMessage(mensaje: String) {
        Thread {
            val nodes = Tasks.await(Wearable.getNodeClient(requireContext()).connectedNodes)
            for (node in nodes) {
                Wearable.getMessageClient(requireContext())
                    .sendMessage(node.id, path, mensaje.toByteArray())
            }
        }.start()
    }
}
