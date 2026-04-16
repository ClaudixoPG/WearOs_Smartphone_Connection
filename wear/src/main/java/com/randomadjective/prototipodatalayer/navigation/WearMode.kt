package com.randomadjective.prototipodatalayer.navigation

enum class WearMode(val route: String, val title: String) {
    TAP("control_1", "Tap"),
    DPAD("control_2", "D-Pad"),
    JOYSTICK("control_3", "Joystick"),
    HOLD("control_4", "Hold"),
    GYRO("sensor_gyro", "Gyroscope"),
    LOCATION("sensor_location", "Location"),
    HEART("sensor_heart", "Heart Rate");

    companion object {
        fun fromRoute(route: String): WearMode? {
            return entries.firstOrNull { it.route == route }
        }
    }
}