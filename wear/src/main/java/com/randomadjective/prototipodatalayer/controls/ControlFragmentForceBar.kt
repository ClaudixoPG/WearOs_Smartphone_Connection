package com.randomadjective.prototipodatalayer.controls

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentForceBar : BaseControlFragment(R.layout.fragment_control_force_bar) {

    private lateinit var barra: ProgressBar
    private lateinit var textoFuerza: TextView
    private val handler = Handler(Looper.getMainLooper())

    private var fuerza = 0f  // ahora float
    private var cargando = false

    // Variables para deltaTime
    private var lastUpdateTime = System.currentTimeMillis()
    private val incrementoPorSegundo = 40f   // velocidad de carga (0–100 por segundo)
    private val decrementoPorSegundo = 20f   // velocidad de descarga (0–100 por segundo)

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        barra = view.findViewById(R.id.progressBar)

        // Texto numérico centrado sobre la barra
        textoFuerza = TextView(requireContext()).apply {
            text = "0.00"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        (view as ViewGroup).addView(textoFuerza)

        // Texto animado (nombre del control)
        val titulo = TextView(requireContext()).apply {
            text = "Control 4: Fuerza"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0f
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        view.addView(titulo)

        titulo.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .withEndAction {
                titulo.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setStartDelay(1000)
                    .withEndAction {
                        view.removeView(titulo)
                    }
                    .start()
            }
            .start()

        // Interacción táctil
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    cargando = true
                    lastUpdateTime = System.currentTimeMillis()
                    enviarMensaje("fuerza:${"%.2f".format(fuerza / 100f)}")
                    handler.post(cargaRunnable)
                }

                MotionEvent.ACTION_UP -> {
                    cargando = false
                    lastUpdateTime = System.currentTimeMillis()
                    enviarMensaje("fuerzaRelease:${"%.2f".format(fuerza / 100f)}")
                    handler.post(descargaRunnable)
                }
            }
            true
        }
    }

    // Aumenta la barra mientras se mantiene presionado
    private val cargaRunnable = object : Runnable {
        override fun run() {
            if (!cargando) return

            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f // en segundos
            lastUpdateTime = currentTime

            if (fuerza < 100f) {
                fuerza += incrementoPorSegundo * deltaTime
                if (fuerza > 100f) fuerza = 100f
                barra.progress = fuerza.toInt()
                textoFuerza.text = "%.2f".format(fuerza / 100f)
            }

            handler.postDelayed(this, 16) // ~60 fps
        }
    }

    // Descarga lentamente cuando se suelta
    private val descargaRunnable = object : Runnable {
        override fun run() {
            if (cargando || fuerza <= 0f) return

            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f // en segundos
            lastUpdateTime = currentTime

            fuerza -= decrementoPorSegundo * deltaTime
            if (fuerza < 0f) fuerza = 0f

            barra.progress = fuerza.toInt()
            textoFuerza.text = "%.2f".format(fuerza / 100f)

            handler.postDelayed(this, 16) // ~60 fps
        }
    }
}
