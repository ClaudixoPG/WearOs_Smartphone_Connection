package com.randomadjective.prototipodatalayer.controls.gameplay

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentDpadGameplay : BaseControlFragment(R.layout.fragment_gameplay_control_dpad) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupColorButton(view.findViewById(R.id.btn_red), "UP")
        setupColorButton(view.findViewById(R.id.btn_blue), "LEFT")
        setupColorButton(view.findViewById(R.id.btn_green), "RIGHT")
        setupColorButton(view.findViewById(R.id.btn_yellow), "DOWN")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupColorButton(button: ImageButton, label: String) {
        button.isClickable = true
        button.isFocusable = true
        button.isSoundEffectsEnabled = false

        button.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    sendMessage("Dpad:$label")
                    true
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    sendMessage("DpadRelease:$label")
                    v.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    sendMessage("DpadRelease:$label")
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val inside = event.x in 0f..v.width.toFloat() &&
                            event.y in 0f..v.height.toFloat()

                    if (!inside && v.isPressed) {
                        v.isPressed = false
                        sendMessage("DpadRelease:$label")
                    } else if (inside && !v.isPressed) {
                        v.isPressed = true
                        sendMessage("Dpad:$label")
                    }
                    true
                }

                else -> false
            }
        }
    }
}