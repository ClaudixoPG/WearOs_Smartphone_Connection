package com.randomadjective.prototipodatalayer.controls.gameplay

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment
import java.util.Locale

class ControlFragmentHoldGameplay : BaseControlFragment(R.layout.fragment_gameplay_control_hold) {

    private lateinit var meterContainer: ViewGroup
    private lateinit var meterFill: View
    private lateinit var hookIndicator: ImageView

    private val handler = Handler(Looper.getMainLooper())

    private var holdValue100 = 0f
    private var charging = false

    private var lastUpdateTime = System.currentTimeMillis()
    private val incrementoPorSegundo = 40f
    private val decrementoPorSegundo = 20f

    private var lastSentTime = 0L
    private val sendIntervalMs = 50L

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        meterContainer = view.findViewById(R.id.meter_container)
        meterFill = view.findViewById(R.id.meter_fill)
        hookIndicator = view.findViewById(R.id.hook_indicator)

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!charging) {
                        charging = true
                        lastUpdateTime = System.currentTimeMillis()
                        handler.removeCallbacks(descargaRunnable)
                        handler.post(cargaRunnable)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    charging = false
                    lastUpdateTime = System.currentTimeMillis()
                    sendHoldMessageImmediate(0f)
                    handler.removeCallbacks(cargaRunnable)
                    handler.post(descargaRunnable)
                }
            }
            true
        }

        meterContainer.post {
            updateUi()
        }
    }

    private val cargaRunnable = object : Runnable {
        override fun run() {
            if (!charging) return

            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f
            lastUpdateTime = currentTime

            if (holdValue100 < 100f) {
                holdValue100 += incrementoPorSegundo * deltaTime
                if (holdValue100 > 100f) holdValue100 = 100f
            }

            updateUi()
            sendHoldMessage(holdValue100 / 100f)
            handler.postDelayed(this, 16L)
        }
    }

    private val descargaRunnable = object : Runnable {
        override fun run() {
            if (charging || holdValue100 <= 0f) return

            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f
            lastUpdateTime = currentTime

            holdValue100 -= decrementoPorSegundo * deltaTime
            if (holdValue100 < 0f) holdValue100 = 0f

            updateUi()
            handler.postDelayed(this, 16L)
        }
    }

    private fun updateUi() {
        val value01 = (holdValue100 / 100f).coerceIn(0f, 1f)

        val containerHeight = meterContainer.height
        val hookHeight = hookIndicator.height

        if (containerHeight <= 0 || hookHeight <= 0) return

        val fillHeight = (containerHeight * value01).toInt()
        meterFill.layoutParams.height = fillHeight
        meterFill.requestLayout()

        val availableTravel = (containerHeight - hookHeight).coerceAtLeast(0)
        val hookPosY = availableTravel * (1f - value01)

        hookIndicator.y = hookPosY
    }

    private fun sendHoldMessage(value01: Float) {
        val now = System.currentTimeMillis()
        if (now - lastSentTime < sendIntervalMs) return
        lastSentTime = now
        sendHoldMessageImmediate(value01)
    }

    private fun sendHoldMessageImmediate(value01: Float) {
        val clamped = value01.coerceIn(0f, 1f)
        val message = String.format(Locale.US, "Hold:%.2f", clamped)
        sendMessage(message)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}