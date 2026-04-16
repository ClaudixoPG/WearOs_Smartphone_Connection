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

        view.findViewById<Button>(R.id.btnTapExample).setOnClickListener {
            listener?.onModeSelected(WearMode.TAP_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnDpadExample).setOnClickListener {
            listener?.onModeSelected(WearMode.DPAD_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnJoystickExample).setOnClickListener {
            listener?.onModeSelected(WearMode.JOYSTICK_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnHoldExample).setOnClickListener {
            listener?.onModeSelected(WearMode.HOLD_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnGyroExample).setOnClickListener {
            listener?.onModeSelected(WearMode.GYRO_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnLocationExample).setOnClickListener {
            listener?.onModeSelected(WearMode.LOCATION_EXAMPLE)
        }
        view.findViewById<Button>(R.id.btnHeartExample).setOnClickListener {
            listener?.onModeSelected(WearMode.HEART_EXAMPLE)
        }

        view.findViewById<Button>(R.id.btnTapGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.TAP_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnDpadGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.DPAD_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnJoystickGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.JOYSTICK_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnHoldGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.HOLD_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnGyroGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.GYRO_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnLocationGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.LOCATION_GAMEPLAY)
        }
        view.findViewById<Button>(R.id.btnHeartGameplay).setOnClickListener {
            listener?.onModeSelected(WearMode.HEART_GAMEPLAY)
        }
    }
}