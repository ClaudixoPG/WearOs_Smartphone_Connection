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

    private fun lanzarNotificacion(mensaje: String) {
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




///Old code///

/*package com.randomadjective.prototipodatalayer

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets
import android.widget.Button
import android.widget.EditText
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresPermission

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var textView: TextView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: Button
    private val path = "/mensaje"
    private val TAG = "Wear_Main"

    /*@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wear_channel",
                "Mensajes del Smartphone",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        //enviarMensajeAlTelefono("Mensaje enviado desde smartwatch")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val testNotification = NotificationCompat.Builder(this, "wear_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Test Notificación")
            .setContentText("Esto es una prueba")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(999, testNotification)


    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textViewWear)
        inputMessage = findViewById(R.id.inputMessageWear)
        sendButton = findViewById(R.id.buttonSendWear)

        // Solicitar permiso POST_NOTIFICATIONS si es necesario (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        // Crear canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wear_channel",
                "Mensajes del Smartphone",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        /*// Botón para enviar mensaje al teléfono
        sendButton.setOnClickListener {
            val mensaje = inputMessage.text.toString()
            enviarMensajeAlTelefono(mensaje)

            // Notificación de prueba
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val testNotification = NotificationCompat.Builder(this, "wear_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Notificación de prueba")
                .setContentText("Este es un mensaje de prueba.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(999, testNotification)
        }*/
        sendButton.setOnClickListener {
            val mensaje = inputMessage.text.toString()
            enviarMensajeAlTelefono(mensaje)

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, "wear_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Notificación de prueba")
                .setContentText("Mensaje: $mensaje")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            runOnUiThread {
                NotificationManagerCompat.from(this).notify(999, notification)
            }
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

    /*@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == path) {
            val mensaje = String(event.data, StandardCharsets.UTF_8)
            //Log.i(TAG, "Mensaje recibido: $mensaje")

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

            runOnUiThread {
                textView.text = "Recibido: $mensaje"
            }
        }
    }*/

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == path) {
            val mensaje = String(event.data, StandardCharsets.UTF_8)
            Log.i(TAG, "Mensaje recibido: $mensaje")

            // Validar que NO sea un mensaje que nosotros mismos enviamos
            Wearable.getNodeClient(this).localNode.addOnSuccessListener { localNode ->
                if (event.sourceNodeId != localNode.id) {
                    // Solo lanzar notificación si viene de otro dispositivo (ej: smartphone)
                    lanzarNotificacion(mensaje)
                } else {
                    Log.i(TAG, "Mensaje local recibido, no se mostrará notificación.")
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

        val notification = NotificationCompat.Builder(this, "wear_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo mensaje")
            .setContentText(mensaje)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
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
}*/