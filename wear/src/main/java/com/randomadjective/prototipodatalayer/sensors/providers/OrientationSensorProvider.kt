package com.randomadjective.prototipodatalayer.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class OrientationSensorProvider(
    context: Context,
    private val onHeadingChanged: (headingDegrees: Float) -> Unit,
    private val onAvailabilityChanged: (available: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private var isRunning = false

    fun start() {
        if (isRunning) return

        if (rotationVectorSensor == null) {
            onAvailabilityChanged(false)
            return
        }

        onAvailabilityChanged(true)

        sensorManager.registerListener(
            this,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        isRunning = true
    }

    fun stop() {
        if (!isRunning) return

        sensorManager.unregisterListener(this)
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        val azimuthRadians = orientationValues[0]
        val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        val normalizedHeading = normalizeDegrees(azimuthDegrees)

        onHeadingChanged(normalizedHeading)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Por ahora no usamos accuracy. Se puede agregar después si queremos mostrar calibración.
    }

    private fun normalizeDegrees(value: Float): Float {
        var result = value % 360f
        if (result < 0f) result += 360f
        return result.roundToInt().toFloat()
    }
}