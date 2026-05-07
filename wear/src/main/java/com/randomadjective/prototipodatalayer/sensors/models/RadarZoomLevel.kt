package com.randomadjective.prototipodatalayer.sensors.models

enum class RadarZoomLevel(
    val label: String,
    val metersPerCell: Float
) {
    FAR("10 m/celda", 10f),
    MEDIUM("5 m/celda", 5f),
    NEAR("1 m/celda", 1f);

    fun nextOrNull(): RadarZoomLevel? {
        return when (this) {
            FAR -> MEDIUM
            MEDIUM -> NEAR
            NEAR -> null
        }
    }
}