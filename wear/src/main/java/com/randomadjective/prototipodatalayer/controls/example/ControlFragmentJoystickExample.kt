package com.randomadjective.prototipodatalayer.controls.example

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
import java.util.Locale

class ControlFragmentJoystickExample : BaseControlFragment(R.layout.fragment_example_control_joystick) {

    private var lastSentTime = 0L
    private val sendIntervalMs = 50L

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stick = view.findViewById<View>(R.id.stick)
        val container = view.findViewById<FrameLayout>(R.id.joystick_container)

        val title = TextView(requireContext()).apply {
            text = "Control 3: Joystick Example"
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
                    .withEndAction { (view as ViewGroup).removeView(title) }
                    .start()
            }
            .start()

        container.setOnTouchListener { _, event ->
            val w = container.width.toFloat()
            val h = container.height.toFloat()
            val xCenter = w / 2
            val yCenter = h / 2

            val x = event.x
            val y = event.y

            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val dx = x - xCenter
                    val dy = y - yCenter

                    stick.translationX = dx
                    stick.translationY = dy

                    val xNorm = ((x / w) - 0.5f) * 2f
                    val yNorm = ((y / h) - 0.5f) * -2f

                    sendJoystickMessage(xNorm, yNorm)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val animX = ObjectAnimator.ofFloat(stick, "translationX", stick.translationX, 0f)
                    val animY = ObjectAnimator.ofFloat(stick, "translationY", stick.translationY, 0f)

                    animX.interpolator = BounceInterpolator()
                    animY.interpolator = BounceInterpolator()
                    animX.duration = 300
                    animY.duration = 300

                    animX.start()
                    animY.start()

                    sendMessage("JoystickRelease:0.00,0.00")
                }
            }
            true
        }
    }

    private fun sendJoystickMessage(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastSentTime < sendIntervalMs) return

        lastSentTime = now

        val xClamped = x.coerceIn(-1f, 1f)
        val yClamped = y.coerceIn(-1f, 1f)

        val message = String.format(Locale.US, "Joystick:%.2f,%.2f", xClamped, yClamped)
        sendMessage(message)
    }
}