package com.randomadjective.prototipodatalayer.sensors.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.sensors.models.GyroscopeUiState
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus
import com.randomadjective.prototipodatalayer.sensors.models.ViewMode
import com.randomadjective.prototipodatalayer.sensors.providers.GyroscopeSensorProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GyroscopeSensorFragment : Fragment(R.layout.fragment_sensor_gyroscope) {

    private lateinit var btnToggleViewMode: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView
    private lateinit var tvMagnitude: TextView
    private lateinit var tvMovementLabel: TextView
    private lateinit var tvTimestamp: TextView

    private lateinit var gyroscopeProvider: GyroscopeSensorProvider

    private var uiState = GyroscopeUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnToggleViewMode = view.findViewById(R.id.btnToggleViewMode)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvX = view.findViewById(R.id.tvX)
        tvY = view.findViewById(R.id.tvY)
        tvZ = view.findViewById(R.id.tvZ)
        tvMagnitude = view.findViewById(R.id.tvMagnitude)
        tvMovementLabel = view.findViewById(R.id.tvMovementLabel)
        tvTimestamp = view.findViewById(R.id.tvTimestamp)

        gyroscopeProvider = GyroscopeSensorProvider(
            context = requireContext(),
            onStatusChanged = { status ->
                uiState = uiState.copy(status = status)
                render()
            },
            onReadingChanged = { x, y, z, magnitude, timestamp ->
                uiState = uiState.copy(
                    x = x,
                    y = y,
                    z = z,
                    magnitude = magnitude,
                    movementLabel = calculateMovementLabel(magnitude),
                    lastUpdateTimestamp = timestamp
                )
                render()
            }
        )

        btnToggleViewMode.setOnClickListener {
            uiState = uiState.copy(
                viewMode = if (uiState.viewMode == ViewMode.RAW) {
                    ViewMode.PROCESSED
                } else {
                    ViewMode.RAW
                }
            )
            render()
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        gyroscopeProvider.start()
    }

    override fun onPause() {
        super.onPause()
        gyroscopeProvider.stop()
    }

    private fun render() {
        btnToggleViewMode.text = "Vista: ${uiState.viewMode.name}"
        tvStatus.text = "Estado: ${uiState.status.name}"

        when (uiState.status) {
            SensorStatus.ACTIVE -> tvStatus.setTextColor(Color.GREEN)
            SensorStatus.NOT_AVAILABLE,
            SensorStatus.ERROR -> tvStatus.setTextColor(Color.RED)
            else -> tvStatus.setTextColor(Color.WHITE)
        }

        when (uiState.viewMode) {
            ViewMode.RAW -> {
                tvX.visibility = View.VISIBLE
                tvY.visibility = View.VISIBLE
                tvZ.visibility = View.VISIBLE

                tvX.text = "X: ${formatFloat(uiState.x)}"
                tvY.text = "Y: ${formatFloat(uiState.y)}"
                tvZ.text = "Z: ${formatFloat(uiState.z)}"
                tvMagnitude.text = "Magnitud: ${formatFloat(uiState.magnitude)}"
                tvMovementLabel.text = "Movimiento: ${uiState.movementLabel}"
            }

            ViewMode.PROCESSED -> {
                tvX.visibility = View.GONE
                tvY.visibility = View.GONE
                tvZ.visibility = View.GONE

                tvMagnitude.text = "Magnitud: ${formatFloat(uiState.magnitude)}"
                tvMovementLabel.text = "Movimiento: ${uiState.movementLabel}"
            }
        }

        tvTimestamp.text = "Última lectura: ${formatTimestamp(uiState.lastUpdateTimestamp)}"
    }

    private fun calculateMovementLabel(magnitude: Float): String {
        return when {
            magnitude < 0.15f -> "Stable"
            magnitude < 1.0f -> "Moving"
            else -> "High rotation"
        }
    }

    private fun formatFloat(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}
