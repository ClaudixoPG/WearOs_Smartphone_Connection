package com.randomadjective.prototipodatalayer.sensors.models

data class HeartRateUiState(
    val status: SensorStatus = SensorStatus.INACTIVE,
    val viewMode: ViewMode = ViewMode.RAW,
    val bpm: Float = 0f,
    val smoothedBpm: Float = 0f,
    val hasPermission: Boolean = false,
    val contactState: String = "Sin datos",
    val lastUpdateTimestamp: Long = 0L
)