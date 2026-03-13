package com.randomadjective.prototipodatalayer.sensors.models

data class LocationUiState(
    val status: SensorStatus = SensorStatus.INACTIVE,
    val viewMode: ViewMode = ViewMode.RAW,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val altitude: Double = 0.0,
    val hasPermission: Boolean = false,
    val movementLabel: String = "Sin datos",
    val lastUpdateTimestamp: Long = 0L
)
