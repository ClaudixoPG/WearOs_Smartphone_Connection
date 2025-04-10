package com.randomadjective.prototipodatalayer

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets
import android.widget.Button
import android.widget.EditText

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var textView: TextView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: Button
    private val path = "/mensaje"
    private val TAG = "Phone_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textViewPhone)
        inputMessage = findViewById(R.id.inputMessagePhone)
        sendButton = findViewById(R.id.buttonSendPhone)

        sendButton.setOnClickListener {
            val mensaje = inputMessage.text.toString()
            enviarMensajeAlReloj(mensaje)
        }

        enviarMensajeAlReloj("Mensaje enviado desde smartphone")
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == path) {
            val mensaje = String(event.data, StandardCharsets.UTF_8)
            Log.i(TAG, "Mensaje recibido: $mensaje")
            runOnUiThread {
                textView.text = "Recibido: $mensaje"
            }
        }
    }

    private fun enviarMensajeAlReloj(mensaje: String) {
        Thread {
            val nodeListTask = Wearable.getNodeClient(this).connectedNodes
            val nodes = Tasks.await(nodeListTask)
            for (node in nodes) {
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, path, mensaje.toByteArray())
                Log.i(TAG, "Mensaje enviado a ${node.displayName}")
            }
        }.start()
    }
}
