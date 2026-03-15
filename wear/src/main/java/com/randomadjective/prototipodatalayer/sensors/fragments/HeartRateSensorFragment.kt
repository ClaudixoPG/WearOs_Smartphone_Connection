package com.randomadjective.prototipodatalayer.sensors.fragments

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.sensors.models.HeartRateUiState
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus
import com.randomadjective.prototipodatalayer.sensors.models.ViewMode
import com.randomadjective.prototipodatalayer.sensors.providers.HeartRateSensorProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateSensorFragment : Fragment(R.layout.fragment_sensor_heart_rate) {

    private lateinit var btnToggleViewMode: Button
    private lateinit var tvPermission: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBpm: TextView
    private lateinit var tvSmoothedBpm: TextView
    private lateinit var tvContactState: TextView
    private lateinit var tvTimestamp: TextView

    private lateinit var heartRateProvider: HeartRateSensorProvider

    private var uiState = HeartRateUiState()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = requiredHeartRatePermissions().any { permissions[it] == true }

            uiState = uiState.copy(hasPermission = granted)
            render()

            if (granted) {
                heartRateProvider.start()
            } else {
                uiState = uiState.copy(status = SensorStatus.ERROR)
                render()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnToggleViewMode = view.findViewById(R.id.btnToggleViewMode)
        tvPermission = view.findViewById(R.id.tvPermission)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvBpm = view.findViewById(R.id.tvBpm)
        tvSmoothedBpm = view.findViewById(R.id.tvSmoothedBpm)
        tvContactState = view.findViewById(R.id.tvContactState)
        tvTimestamp = view.findViewById(R.id.tvTimestamp)

        heartRateProvider = HeartRateSensorProvider(
            context = requireContext(),
            onStatusChanged = { status ->
                uiState = uiState.copy(status = status)
                render()
            },
            onReadingChanged = { bpm, smoothedBpm, contactState, timestamp ->
                uiState = uiState.copy(
                    bpm = bpm,
                    smoothedBpm = smoothedBpm,
                    contactState = contactState,
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
        if (hasHeartRatePermission()) {
            uiState = uiState.copy(hasPermission = true)
            render()
            heartRateProvider.start()
        } else {
            requestHeartRatePermission()
        }
    }

    override fun onPause() {
        super.onPause()
        heartRateProvider.stop()
    }

    private fun requiredHeartRatePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 36) {
            arrayOf("android.permission.health.READ_HEART_RATE")
        } else {
            arrayOf("android.permission.BODY_SENSORS")
        }
    }

    private fun hasHeartRatePermission(): Boolean {
        return requiredHeartRatePermissions().any { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestHeartRatePermission() {
        permissionLauncher.launch(requiredHeartRatePermissions())
    }

    private fun render() {
        btnToggleViewMode.text = "Vista: ${uiState.viewMode.name}"
        tvPermission.text = "Permiso: ${if (uiState.hasPermission) "GRANTED" else "DENIED"}"
        tvStatus.text = "Estado: ${uiState.status.name}"

        tvPermission.setTextColor(if (uiState.hasPermission) Color.GREEN else Color.RED)

        when (uiState.status) {
            SensorStatus.ACTIVE -> tvStatus.setTextColor(Color.GREEN)
            SensorStatus.ERROR,
            SensorStatus.NOT_AVAILABLE -> tvStatus.setTextColor(Color.RED)
            else -> tvStatus.setTextColor(Color.WHITE)
        }

        when (uiState.viewMode) {
            ViewMode.RAW -> {
                tvBpm.text = "BPM: ${formatFloat(uiState.bpm)}"
                tvSmoothedBpm.visibility = View.VISIBLE
                tvSmoothedBpm.text = "BPM suavizado: ${formatFloat(uiState.smoothedBpm)}"
                tvContactState.text = "Contacto: ${uiState.contactState}"
            }

            ViewMode.PROCESSED -> {
                tvBpm.text = "BPM procesado: ${formatFloat(uiState.smoothedBpm)}"
                tvSmoothedBpm.visibility = View.GONE
                tvContactState.text = "Estado lectura: ${uiState.contactState}"
            }
        }

        tvTimestamp.text = "Última lectura: ${formatTimestamp(uiState.lastUpdateTimestamp)}"
    }

    private fun formatFloat(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}