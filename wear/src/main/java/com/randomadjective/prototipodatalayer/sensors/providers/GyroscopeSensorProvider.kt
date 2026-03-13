package com.randomadjective.prototipodatalayer.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus
import kotlin.math.sqrt

class GyroscopeSensorProvider(
    context: Context,
    private val onStatusChanged: (SensorStatus) -> Unit,
    private val onReadingChanged: (x: Float, y: Float, z: Float, magnitude: Float, timestamp: Long) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gyroscope: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isListening = false

    fun start() {
        if (gyroscope == null) {
            onStatusChanged(SensorStatus.NOT_AVAILABLE)
            Log.w(TAG, "El giroscopio no está disponible en este dispositivo.")
            return
        }

        if (isListening) return

        val registered = sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_UI
        )

        if (registered) {
            isListening = true
            onStatusChanged(SensorStatus.ACTIVE)
            Log.i(TAG, "GyroscopeSensorProvider iniciado.")
        } else {
            onStatusChanged(SensorStatus.ERROR)
            Log.e(TAG, "No se pudo registrar el listener del giroscopio.")
        }
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
        onStatusChanged(SensorStatus.INACTIVE)
        Log.i(TAG, "GyroscopeSensorProvider detenido.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val timestamp = System.currentTimeMillis()

        onReadingChanged(x, y, z, magnitude, timestamp)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión del giroscopio cambió: $accuracy")
    }

    companion object {
        private const val TAG = "GyroProvider"
    }
}
