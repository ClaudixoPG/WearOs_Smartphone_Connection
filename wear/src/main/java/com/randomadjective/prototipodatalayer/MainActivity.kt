package com.randomadjective.prototipodatalayer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.fragment.app.Fragment
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.randomadjective.prototipodatalayer.base.TelemetryEnvelope
import com.randomadjective.prototipodatalayer.base.WearMessageSender
import com.randomadjective.prototipodatalayer.navigation.ModeMenuFragment
import com.randomadjective.prototipodatalayer.navigation.WearMode
import com.randomadjective.prototipodatalayer.navigation.WearModeFactory
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(),
    MessageClient.OnMessageReceivedListener,
    ModeMenuFragment.Listener {

    private val path = "/mensaje"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wear_channel",
                "Mensajes del Smartphone",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        if (savedInstanceState == null) {
            showModeMenu()
        }
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

    override fun onModeSelected(mode: WearMode) {
        navigateToMode(mode, addToBackStack = true)
    }

    private fun showModeMenu() {
        enableImmersiveMode()

        findViewById<TextView>(R.id.defaultMessage)?.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out
            )
            .replace(R.id.fragment_container, ModeMenuFragment())
            .commit()
    }

    private fun navigateToMode(mode: WearMode, addToBackStack: Boolean) {
        val fragment = WearModeFactory.create(mode)
        showFragment(fragment, mode.route, addToBackStack)
    }

    private fun showFragment(fragment: Fragment, backStackName: String?, addToBackStack: Boolean) {
        enableImmersiveMode()

        findViewById<TextView>(R.id.defaultMessage)?.visibility = View.GONE

        val tx = supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out
            )
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack && backStackName != null) {
            tx.addToBackStack(backStackName)
        }

        tx.commit()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_2,
                KeyEvent.KEYCODE_STEM_3 -> return handlePhysicalBack()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handlePhysicalBack(): Boolean {
        return if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            true
        } else {
            false
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != path) return

        val mensaje = String(event.data, StandardCharsets.UTF_8)

        if (TelemetryEnvelope.isJson(mensaje)) {
            val recordType = TelemetryEnvelope.getRecordType(mensaje)
            if (recordType == "input_ack") {
                val handled = WearMessageSender.handleIncomingAck(this, mensaje)
                if (handled) return
            }
        }

        Wearable.getNodeClient(this).localNode.addOnSuccessListener { localNode ->
            if (event.sourceNodeId != localNode.id) {
                lanzarNotificacion(mensaje)
            }

            runOnUiThread {
                val mode = WearMode.fromRoute(mensaje)
                if (mode != null) {
                    navigateToMode(mode, addToBackStack = true)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun lanzarNotificacion(mensaje: String) {
        val notiMode = 2

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
            builder.setContentIntent(pendingIntent)
                .addAction(
                    NotificationCompat.Action.Builder(
                        R.mipmap.ic_launcher,
                        "Ver mensaje",
                        pendingIntent
                    ).build()
                )
        } else {
            val remoteInput = RemoteInput.Builder("respuesta")
                .setLabel("Responder mensaje")
                .build()

            val replyIntent = Intent(this, MainActivity::class.java)

            val replyPendingIntent = PendingIntent.getActivity(
                this,
                1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val replyAction = NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Responder",
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            builder.addAction(replyAction)
        }

        NotificationManagerCompat.from(this).notify(1, builder.build())
    }
}