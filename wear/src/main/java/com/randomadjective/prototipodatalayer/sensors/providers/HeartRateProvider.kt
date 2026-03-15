package com.randomadjective.prototipodatalayer.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus

class HeartRateSensorProvider(
    context: Context,
    private val onStatusChanged: (SensorStatus) -> Unit,
    private val onReadingChanged: (bpm: Float, smoothedBpm: Float, contactState: String, timestamp: Long) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val heartRateSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private var isListening = false
    private var smoothedBpm = 0f
    private var hasSmoothedValue = false

    fun start() {
        if (heartRateSensor == null) {
            onStatusChanged(SensorStatus.NOT_AVAILABLE)
            Log.w(TAG, "El sensor de ritmo cardiaco no está disponible en este dispositivo.")
            return
        }

        if (isListening) return

        val registered = sensorManager.registerListener(
            this,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (registered) {
            isListening = true
            onStatusChanged(SensorStatus.ACTIVE)
            Log.i(TAG, "HeartRateSensorProvider iniciado.")
        } else {
            onStatusChanged(SensorStatus.ERROR)
            Log.e(TAG, "No se pudo registrar el listener del sensor cardiaco.")
        }
    }

    fun stop() {
        if (!isListening) return

        sensorManager.unregisterListener(this)
        isListening = false
        onStatusChanged(SensorStatus.INACTIVE)
        Log.i(TAG, "HeartRateSensorProvider detenido.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_HEART_RATE) return
        if (event.values.isEmpty()) return

        val bpm = event.values[0]
        val timestamp = System.currentTimeMillis()

        val contactState = when {
            bpm <= 0f -> "Sin contacto o sin lectura válida"
            bpm < 40f -> "Lectura baja / revisar contacto"
            else -> "Lectura válida"
        }

        smoothedBpm = if (!hasSmoothedValue) {
            hasSmoothedValue = true
            bpm
        } else {
            // suavizado simple
            (smoothedBpm * 0.8f) + (bpm * 0.2f)
        }

        onReadingChanged(
            bpm,
            smoothedBpm,
            contactState,
            timestamp
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión del sensor cardiaco cambió: $accuracy")
    }

    companion object {
        private const val TAG = "HeartRateProvider"
    }
}