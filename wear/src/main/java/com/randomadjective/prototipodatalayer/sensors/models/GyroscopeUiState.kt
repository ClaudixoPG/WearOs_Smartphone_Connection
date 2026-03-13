package com.randomadjective.prototipodatalayer.sensors.models

data class GyroscopeUiState(
    val status: SensorStatus = SensorStatus.INACTIVE,
    val viewMode: ViewMode = ViewMode.RAW,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val movementLabel: String = "Sin datos",
    val lastUpdateTimestamp: Long = 0L
)
