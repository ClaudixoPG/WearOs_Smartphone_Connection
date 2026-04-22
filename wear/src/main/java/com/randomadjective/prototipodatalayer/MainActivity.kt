package com.randomadjective.prototipodatalayer

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
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

        if (savedInstanceState == null) {
            showModeMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        Wearable.getMessageClient(this).addListener(this)
        WearMessageSender.warmup(this)
    }

    override fun onPause() {
        Wearable.getMessageClient(this).removeListener(this)
        super.onPause()
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

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != path) return

        val message = String(event.data, StandardCharsets.UTF_8)
        println("message is:" + message)

        // 1) ACK de medición
        if (WearMessageSender.handleIncomingMessage(this, message)) {
            return
        }

        // 2) Inicio/fin de sesión general de prueba
        if (message == "TEST_SESSION_START") {
            WearMessageSender.startTestSession(this)
            return
        }

        if (message == "TEST_SESSION_END") {
            WearMessageSender.endTestSession(this)
            return
        }

        // 3) Inicio/fin de sesión por minijuego
        if (message.startsWith("MINIGAME_SESSION_START|")) {
            val minigameId = message.substringAfter("MINIGAME_SESSION_START|", "").trim()
            if (minigameId.isNotEmpty()) {
                WearMessageSender.startMinigameSession(this, minigameId)
            }
            return
        }

        if (message == "MINIGAME_SESSION_END") {
            WearMessageSender.endMinigameSession(this)
            return
        }

        // 4) Navegación / control antiguo
        runOnUiThread {
            val mode = WearMode.fromRoute(message)
            if (mode != null) {
                navigateToMode(mode, addToBackStack = true)
            }
        }
    }
}