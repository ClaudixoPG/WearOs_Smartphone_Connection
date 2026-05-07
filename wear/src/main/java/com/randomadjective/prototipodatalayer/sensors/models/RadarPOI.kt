package com.randomadjective.prototipodatalayer.sensors.models

data class RadarPOI(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var isFound: Boolean = false,
    var lastDetectedPulseId: Int = -1
)