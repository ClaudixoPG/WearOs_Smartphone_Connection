package com.randomadjective.prototipodatalayer.navigation

import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.controls.ControlFragmentDpad
import com.randomadjective.prototipodatalayer.controls.ControlFragmentHold
import com.randomadjective.prototipodatalayer.controls.ControlFragmentJoystick
import com.randomadjective.prototipodatalayer.controls.ControlFragmentTap
import com.randomadjective.prototipodatalayer.sensors.fragments.GyroscopeSensorFragment
import com.randomadjective.prototipodatalayer.sensors.fragments.HeartRateSensorFragment
import com.randomadjective.prototipodatalayer.sensors.fragments.LocationSensorFragment

object WearModeFactory {
    fun create(mode: WearMode): Fragment {
        return when (mode) {
            WearMode.TAP -> ControlFragmentTap()
            WearMode.DPAD -> ControlFragmentDpad()
            WearMode.JOYSTICK -> ControlFragmentJoystick()
            WearMode.HOLD -> ControlFragmentHold()
            WearMode.GYRO -> GyroscopeSensorFragment()
            WearMode.LOCATION -> LocationSensorFragment()
            WearMode.HEART -> HeartRateSensorFragment()
        }
    }
}