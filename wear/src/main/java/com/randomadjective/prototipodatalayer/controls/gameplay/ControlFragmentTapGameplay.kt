package com.randomadjective.prototipodatalayer.controls.gameplay

import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentTapGameplay : BaseControlFragment(R.layout.fragment_gameplay_control_tap) {

    private val useSoundFeedback = false //sonido
    private val useHapticFeedback = false //vibración

    private var feedbackOverlay: View? = null
    private var centerLabel: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = view as ViewGroup

        root.isClickable = true
        root.isFocusable = true
        root.setBackgroundColor(Color.BLACK)

        setupCenterLabel(root)
        setupFeedbackOverlay(root)

        root.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    playTapFeedback(root)
                    sendMessage("Tap")
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    true
                }

                else -> false
            }
        }
    }

    private fun setupCenterLabel(root: ViewGroup) {
        centerLabel = TextView(requireContext()).apply {
            text = "TAP"
            textSize = 22f
            setTextColor(Color.WHITE)
            alpha = 0.85f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        root.addView(
            centerLabel,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        centerLabel?.apply {
            scaleX = 1f
            scaleY = 1f
        }
    }

    private fun setupFeedbackOverlay(root: ViewGroup) {
        feedbackOverlay = View(requireContext()).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0f
            isClickable = false
            isFocusable = false
        }

        root.addView(
            feedbackOverlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun playTapFeedback(root: ViewGroup) {
        if (useSoundFeedback) {
            root.playSoundEffect(SoundEffectConstants.CLICK)
        }

        if (useHapticFeedback) {
            root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        feedbackOverlay?.animate()?.cancel()
        centerLabel?.animate()?.cancel()

        feedbackOverlay?.apply {
            alpha = 0f
            animate()
                .alpha(0.18f)
                .setDuration(60)
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(140)
                        .start()
                }
                .start()
        }

        centerLabel?.apply {
            scaleX = 1f
            scaleY = 1f
            animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(70)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
        }
    }
}