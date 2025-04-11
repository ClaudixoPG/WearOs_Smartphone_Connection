package com.randomadjective.prototipodatalayer

import android.Manifest
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
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var textView: TextView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: Button
    private val path = "/mensaje"
    private val TAG = "Wear_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textViewWear)
        inputMessage = findViewById(R.id.inputMessageWear)
        sendButton = findViewById(R.id.buttonSendWear)

        sendButton.setOnClickListener {
            val mensaje = inputMessage.text.toString()
            enviarMensajeAlTelefono(mensaje)
        }

        // Crear canal de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wear_channel",
                "Mensajes del Smartphone",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == path) {
            val mensaje = String(event.data, StandardCharsets.UTF_8)
            Log.i(TAG, "Mensaje recibido: $mensaje")

            // Validar si viene de otro dispositivo (smartphone)
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

    /*private fun lanzarNotificacion(mensaje: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "wear_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo mensaje")
            .setContentText(mensaje)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
    }*/

    private fun lanzarNotificacion(mensaje: String) {
        val notiMode = 2 // Cambia entre 1 (botón simple) o 2 (respuesta rápida)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "wear_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo mensaje")
            .setContentText(mensaje)
            .setAutoCancel(true)

        if (notiMode == 1) {
            // MODO 1: Botón de acción
            builder.setContentIntent(pendingIntent)
                .addAction(
                    NotificationCompat.Action.Builder(
                        //R.drawable.ic_launcher_foreground, // ícono
                        R.mipmap.ic_launcher, // ícono
                        "Ver mensaje",
                        pendingIntent
                    ).build()
                )
        } else if (notiMode == 2) {
            // MODO 2: RemoteInput para respuesta rápida
            val remoteInput = androidx.core.app.RemoteInput.Builder("respuesta")
                .setLabel("Responder mensaje")
                .build()

            val replyIntent = Intent(this, MainActivity::class.java)
            /*val replyPendingIntent = PendingIntent.getActivity(
                this, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )*/

            val replyPendingIntent = PendingIntent.getActivity(
                this,
                1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE// ← Solo esta bandera (mutable)
            )

            val replyAction = NotificationCompat.Action.Builder(
                //R.drawable.ic_launcher_foreground,
                R.mipmap.ic_launcher,
                "Responder", replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            builder.addAction(replyAction)
        }

        NotificationManagerCompat.from(this).notify(1, builder.build())
    }


    private fun enviarMensajeAlTelefono(mensaje: String) {
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