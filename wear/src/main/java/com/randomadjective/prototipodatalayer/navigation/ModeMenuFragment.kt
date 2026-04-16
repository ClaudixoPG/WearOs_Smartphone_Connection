package com.randomadjective.prototipodatalayer.navigation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.R

class ModeMenuFragment : Fragment(R.layout.fragment_mode_menu) {

    interface Listener {
        fun onModeSelected(mode: WearMode)
    }

    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnTap).setOnClickListener {
            listener?.onModeSelected(WearMode.TAP)
        }

        view.findViewById<Button>(R.id.btnDpad).setOnClickListener {
            listener?.onModeSelected(WearMode.DPAD)
        }

        view.findViewById<Button>(R.id.btnJoystick).setOnClickListener {
            listener?.onModeSelected(WearMode.JOYSTICK)
        }

        view.findViewById<Button>(R.id.btnHold).setOnClickListener {
            listener?.onModeSelected(WearMode.HOLD)
        }

        view.findViewById<Button>(R.id.btnGyro).setOnClickListener {
            listener?.onModeSelected(WearMode.GYRO)
        }

        view.findViewById<Button>(R.id.btnLocation).setOnClickListener {
            listener?.onModeSelected(WearMode.LOCATION)
        }

        view.findViewById<Button>(R.id.btnHeart).setOnClickListener {
            listener?.onModeSelected(WearMode.HEART)
        }
    }
}