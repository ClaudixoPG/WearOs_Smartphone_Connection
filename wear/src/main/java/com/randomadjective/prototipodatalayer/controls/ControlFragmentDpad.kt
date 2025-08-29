package com.randomadjective.prototipodatalayer.controls

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentDpad : BaseControlFragment(R.layout.fragment_control_dpad) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        @SuppressLint("ClickableViewAccessibility")
        fun setupDpadButton(button: Button, label: String) {
            // Asegura que el botón pueda recibir foco/touch
            button.isClickable = true
            button.isFocusable = true

            button.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // Evita que el padre (ScrollView, etc.) intercepte el gesto
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        v.isPressed = true
                        enviarMensaje("Dpad:$label")
                        true // Consumimos el evento para seguir recibiendo MOVE/UP
                    }

                    MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        enviarMensaje("DpadRelease:$label")
                        v.performClick() // accesibilidad
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        enviarMensaje("DpadRelease:$label")
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // (Opcional) si el dedo sale del área, tratamos como cancel
                        val inside = event.x in 0f..v.width.toFloat() && event.y in 0f..v.height.toFloat()
                        if (!inside && v.isPressed) {
                            v.isPressed = false
                            enviarMensaje("DpadRelease:$label")
                        } else if (inside && !v.isPressed) {
                            v.isPressed = true
                            enviarMensaje("Dpad:$label")
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        setupDpadButton(view.findViewById(R.id.btn_up), "UP")
        setupDpadButton(view.findViewById(R.id.btn_down), "DOWN")
        setupDpadButton(view.findViewById(R.id.btn_left), "LEFT")
        setupDpadButton(view.findViewById(R.id.btn_right), "RIGHT")
        setupDpadButton(view.findViewById(R.id.btn_action), "FIRE")

        // Texto de retroalimentación (igual que antes)
        val title = TextView(requireContext()).apply {
            text = "Control 2: D-Pad"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0f
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        (view as ViewGroup).addView(title)
        title.animate()
            .alpha(1f).setDuration(300).setStartDelay(100)
            .withEndAction {
                title.animate()
                    .alpha(0f).setDuration(300).setStartDelay(1000)
                    .withEndAction { (view as ViewGroup).removeView(title) }
                    .start()
            }.start()
    }
}
