package com.randomadjective.prototipodatalayer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.randomadjective.prototipodatalayer.controls.ControlFragmentDpad
import com.randomadjective.prototipodatalayer.controls.ControlFragmentForceBar
import com.randomadjective.prototipodatalayer.controls.ControlFragmentJoystick
import com.randomadjective.prototipodatalayer.controls.ControlFragmentTap
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private val path = "/mensaje"
    private val TAG = "Wear_Main"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    private fun showControl(fragment: androidx.fragment.app.Fragment) {
        enableImmersiveMode()
        //Print log message in console
        Log.i(TAG, "Cambiando a fragmento: ${fragment.javaClass.simpleName}")

        // Ocultar mensaje por defecto si está visible
        val defaultMessage  = findViewById<TextView>(R.id.defaultMessage)
        defaultMessage ?.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }


    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
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
                    // LogMessage in console
                    Log.i(TAG, "Mensaje recibido: $mensaje")
                    when (mensaje) {
                        "control_1" -> showControl(ControlFragmentTap())
                        "control_2" -> showControl(ControlFragmentDpad())
                        "control_3" -> showControl(ControlFragmentJoystick())
                        "control_4" -> showControl(ControlFragmentForceBar())
                        // Agrega más casos si hay más controles
                        else -> Log.w(TAG, "Mensaje recibido: $mensaje")
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
            val remoteInput = RemoteInput.Builder("respuesta")
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