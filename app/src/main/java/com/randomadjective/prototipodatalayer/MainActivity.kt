package com.randomadjective.prototipodatalayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var textView: TextView
    private val path = "/mensaje"
    private val TAG = "Phone_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textViewPhone)
        val inputMessage = findViewById<EditText>(R.id.inputMessagePhone)
        val sendButton = findViewById<Button>(R.id.buttonSendPhone)

        sendButton.setOnClickListener {
            val mensaje = inputMessage.text.toString()
            enviarMensajeAlReloj(mensaje)
        }

        // Crear canal de notificación si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "phone_channel",
                "Mensajes del Smartwatch",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
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

            // Verificar si el mensaje viene de otro nodo (el reloj)
            Wearable.getNodeClient(this).localNode.addOnSuccessListener { localNode ->
                if (event.sourceNodeId != localNode.id) {
                    lanzarNotificacion(mensaje)
                }

                runOnUiThread {
                    textView.text = "Recibido: $mensaje"
                }
            }
        }
    }

    private fun lanzarNotificacion(mensaje: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "phone_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo mensaje desde el reloj")
            .setContentText(mensaje)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
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




/*package com.randomadjective.prototipodatalayer

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
*/