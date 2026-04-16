package com.randomadjective.prototipodatalayer.navigation

enum class WearMode(
    val route: String,
    val title: String
) {
    TAP_EXAMPLE("control_1", "Tap Example"),
    DPAD_EXAMPLE("control_2", "D-Pad Example"),
    JOYSTICK_EXAMPLE("control_3", "Joystick Example"),
    HOLD_EXAMPLE("control_4", "Hold Example"),
    GYRO_EXAMPLE("sensor_gyro", "Gyroscope Example"),
    LOCATION_EXAMPLE("sensor_location", "Location Example"),
    HEART_EXAMPLE("sensor_heart", "Heart Rate Example"),

    TAP_GAMEPLAY("gameplay_control_1", "Tap Gameplay"),
    DPAD_GAMEPLAY("gameplay_control_2", "D-Pad Gameplay"),
    JOYSTICK_GAMEPLAY("gameplay_control_3", "Joystick Gameplay"),
    HOLD_GAMEPLAY("gameplay_control_4", "Hold Gameplay"),
    GYRO_GAMEPLAY("gameplay_sensor_gyro", "Gyroscope Gameplay"),
    LOCATION_GAMEPLAY("gameplay_sensor_location", "Location Gameplay"),
    HEART_GAMEPLAY("gameplay_sensor_heart", "Heart Rate Gameplay");

    companion object {
        fun fromRoute(route: String): WearMode? {
            return entries.firstOrNull { it.route == route }
        }
    }
}