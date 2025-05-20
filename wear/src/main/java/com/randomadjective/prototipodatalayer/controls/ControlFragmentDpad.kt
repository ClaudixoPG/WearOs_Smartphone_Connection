/*package com.randomadjective.prototipodatalayer.controls

import android.os.Bundle
import android.view.View
import android.widget.Button
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentDpad : BaseControlFragment(R.layout.fragment_control_dpad) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btn_up).setOnClickListener { enviarMensaje("arriba") }
        view.findViewById<Button>(R.id.btn_down).setOnClickListener { enviarMensaje("abajo") }
        view.findViewById<Button>(R.id.btn_left).setOnClickListener { enviarMensaje("izquierda") }
        view.findViewById<Button>(R.id.btn_right).setOnClickListener { enviarMensaje("derecha") }
        view.findViewById<Button>(R.id.btn_action).setOnClickListener { enviarMensaje("Shoot") }
    }
}*/

package com.randomadjective.prototipodatalayer.controls

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.base.BaseControlFragment

class ControlFragmentDpad : BaseControlFragment(R.layout.fragment_control_dpad) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup botones
        view.findViewById<Button>(R.id.btn_up).setOnClickListener { enviarMensaje("arriba") }
        view.findViewById<Button>(R.id.btn_down).setOnClickListener { enviarMensaje("abajo") }
        view.findViewById<Button>(R.id.btn_left).setOnClickListener { enviarMensaje("izquierda") }
        view.findViewById<Button>(R.id.btn_right).setOnClickListener { enviarMensaje("derecha") }
        view.findViewById<Button>(R.id.btn_action).setOnClickListener { enviarMensaje("Shoot") }

        // Agrega texto de retroalimentación
        val title = TextView(requireContext()).apply {
            text = "Control 2: D-Pad"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0f
            //gravity = Gravity.CENTER_HORIZONTAL
            //translationY = -100f  // sube el texto
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        (view as ViewGroup).addView(title)

        // Animación: aparece, espera 1 segundo, desaparece y se elimina
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
    }
}
