package com.randomadjective.prototipodatalayer.navigation

import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.controls.example.ControlFragmentDpadExample
import com.randomadjective.prototipodatalayer.controls.example.ControlFragmentHoldExample
import com.randomadjective.prototipodatalayer.controls.example.ControlFragmentJoystickExample
import com.randomadjective.prototipodatalayer.controls.example.ControlFragmentTapExample
import com.randomadjective.prototipodatalayer.controls.gameplay.ControlFragmentDpadGameplay
import com.randomadjective.prototipodatalayer.controls.gameplay.ControlFragmentHoldGameplay
import com.randomadjective.prototipodatalayer.controls.gameplay.ControlFragmentJoystickGameplay
import com.randomadjective.prototipodatalayer.controls.gameplay.ControlFragmentTapGameplay
import com.randomadjective.prototipodatalayer.sensors.fragments.example.GyroscopeSensorFragmentExample
import com.randomadjective.prototipodatalayer.sensors.fragments.example.HeartRateSensorFragmentExample
import com.randomadjective.prototipodatalayer.sensors.fragments.example.LocationSensorFragmentExample
import com.randomadjective.prototipodatalayer.sensors.fragments.gameplay.GyroscopeSensorFragmentGameplay
import com.randomadjective.prototipodatalayer.sensors.fragments.gameplay.HeartRateSensorFragmentGameplay
import com.randomadjective.prototipodatalayer.sensors.fragments.gameplay.LocationSensorFragmentGameplay

object WearModeFactory {
    fun create(mode: WearMode): Fragment {
        return when (mode) {
            WearMode.TAP_EXAMPLE -> ControlFragmentTapExample()
            WearMode.DPAD_EXAMPLE -> ControlFragmentDpadExample()
            WearMode.JOYSTICK_EXAMPLE -> ControlFragmentJoystickExample()
            WearMode.HOLD_EXAMPLE -> ControlFragmentHoldExample()
            WearMode.GYRO_EXAMPLE -> GyroscopeSensorFragmentExample()
            WearMode.LOCATION_EXAMPLE -> LocationSensorFragmentExample()
            WearMode.HEART_EXAMPLE -> HeartRateSensorFragmentExample()

            WearMode.TAP_GAMEPLAY -> ControlFragmentTapGameplay()
            WearMode.DPAD_GAMEPLAY -> ControlFragmentDpadGameplay()
            WearMode.JOYSTICK_GAMEPLAY -> ControlFragmentJoystickGameplay()
            WearMode.HOLD_GAMEPLAY -> ControlFragmentHoldGameplay()
            WearMode.GYRO_GAMEPLAY -> GyroscopeSensorFragmentGameplay()
            WearMode.LOCATION_GAMEPLAY -> LocationSensorFragmentGameplay()
            WearMode.HEART_GAMEPLAY -> HeartRateSensorFragmentGameplay()
        }
    }
}