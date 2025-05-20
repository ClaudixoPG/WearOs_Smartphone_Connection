package com.randomadjective.prototipodatalayer.controls

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentTap : BaseControlFragment(R.layout.fragment_control_tap) {
    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnClickListener {
            enviarMensaje("Shoot")
        }
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = TextView(requireContext()).apply {
            text = "Control 1: Tap"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0f
            gravity = Gravity.CENTER
        }

        (view as ViewGroup).addView(title)

        // Fade-in animation
        title.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(100)
            .start()

        view.setOnClickListener {
            enviarMensaje("Shoot")
        }
    }


}
