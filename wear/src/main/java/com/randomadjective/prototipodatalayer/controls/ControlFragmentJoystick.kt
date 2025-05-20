/*package com.randomadjective.prototipodatalayer.controls

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentJoystick : BaseControlFragment(R.layout.fragment_control_joystick) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stick = view.findViewById<View>(R.id.stick)
        val container = view.findViewById<FrameLayout>(R.id.joystick_container)

        container.setOnTouchListener { _, event ->
            val w = container.width.toFloat()
            val h = container.height.toFloat()
            val xCenter = w / 2
            val yCenter = h / 2

            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val dx = x - xCenter
                    val dy = y - yCenter

                    // Mover visualmente el "stick"
                    stick.translationX = dx
                    stick.translationY = dy

                    // Calcular normalización
                    val xNorm = ((x / w) - 0.5f) * 2
                    val yNorm = ((y / h) - 0.5f) * -2
                    val mensaje = "joystick:${"%.2f".format(xNorm)},${"%.2f".format(yNorm)}"
                    enviarMensaje(mensaje)
                }

                /*MotionEvent.ACTION_UP -> {
                    // Volver al centro visualmente
                    stick.translationX = 0f
                    stick.translationY = 0f
                    enviarMensaje("joystick:0.00,0.00")
                }*/

                MotionEvent.ACTION_UP -> {
                    val animX = ObjectAnimator.ofFloat(stick, "translationX", stick.translationX, 0f)
                    val animY = ObjectAnimator.ofFloat(stick, "translationY", stick.translationY, 0f)

                    animX.interpolator = BounceInterpolator()
                    animY.interpolator = BounceInterpolator()
                    animX.duration = 300
                    animY.duration = 300

                    animX.start()
                    animY.start()

                    enviarMensaje("joystick:0.00,0.00")
                }
            }
            true
        }
    }
}*/

package com.randomadjective.prototipodatalayer.controls

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentJoystick : BaseControlFragment(R.layout.fragment_control_joystick) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stick = view.findViewById<View>(R.id.stick)
        val container = view.findViewById<FrameLayout>(R.id.joystick_container)

        // Texto animado: "Control 3: Joystick"
        val title = TextView(requireContext()).apply {
            text = "Control 3: Joystick"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0f
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        (view as ViewGroup).addView(title)

        title.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .withEndAction {
                title.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setStartDelay(1000)
                    .withEndAction {
                        (view as ViewGroup).removeView(title)
                    }
                    .start()
            }
            .start()

        // Lógica del joystick
        container.setOnTouchListener { _, event ->
            val w = container.width.toFloat()
            val h = container.height.toFloat()
            val xCenter = w / 2
            val yCenter = h / 2

            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val dx = x - xCenter
                    val dy = y - yCenter

                    stick.translationX = dx
                    stick.translationY = dy

                    val xNorm = ((x / w) - 0.5f) * 2
                    val yNorm = ((y / h) - 0.5f) * -2
                    val mensaje = "joystick:${"%.2f".format(xNorm)},${"%.2f".format(yNorm)}"
                    sendMessage(mensaje)
                }

                MotionEvent.ACTION_UP -> {
                    val animX = ObjectAnimator.ofFloat(stick, "translationX", stick.translationX, 0f)
                    val animY = ObjectAnimator.ofFloat(stick, "translationY", stick.translationY, 0f)

                    animX.interpolator = BounceInterpolator()
                    animY.interpolator = BounceInterpolator()
                    animX.duration = 300
                    animY.duration = 300

                    animX.start()
                    animY.start()

                    sendMessage("joystick:0.00,0.00")
                }
            }
            true
        }
    }
}
