package com.randomadjective.prototipodatalayer.sensors.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.sensors.models.LocationUiState
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus
import com.randomadjective.prototipodatalayer.sensors.models.ViewMode
import com.randomadjective.prototipodatalayer.sensors.providers.LocationSensorProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationSensorFragment : Fragment(R.layout.fragment_sensor_location) {

    private lateinit var btnToggleViewMode: Button
    private lateinit var tvPermission: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvMovementLabel: TextView
    private lateinit var tvTimestamp: TextView

    private lateinit var locationProvider: LocationSensorProvider

    private var uiState = LocationUiState()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            uiState = uiState.copy(hasPermission = granted)
            render()

            if (granted) {
                locationProvider.start()
            } else {
                uiState = uiState.copy(status = SensorStatus.ERROR)
                render()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnToggleViewMode = view.findViewById(R.id.btnToggleViewMode)
        tvPermission = view.findViewById(R.id.tvPermission)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvLatitude = view.findViewById(R.id.tvLatitude)
        tvLongitude = view.findViewById(R.id.tvLongitude)
        tvAccuracy = view.findViewById(R.id.tvAccuracy)
        tvSpeed = view.findViewById(R.id.tvSpeed)
        tvAltitude = view.findViewById(R.id.tvAltitude)
        tvMovementLabel = view.findViewById(R.id.tvMovementLabel)
        tvTimestamp = view.findViewById(R.id.tvTimestamp)

        locationProvider = LocationSensorProvider(
            context = requireContext(),
            onStatusChanged = { status ->
                uiState = uiState.copy(status = status)
                render()
            },
            onPermissionChanged = { granted ->
                uiState = uiState.copy(hasPermission = granted)
                render()
            },
            onLocationChanged = { latitude, longitude, accuracy, speed, altitude, timestamp ->
                uiState = uiState.copy(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    speed = speed,
                    altitude = altitude,
                    movementLabel = calculateMovementLabel(speed),
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
        if (hasLocationPermission()) {
            locationProvider.start()
        } else {
            requestLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        locationProvider.stop()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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
                tvLatitude.visibility = View.VISIBLE
                tvLongitude.visibility = View.VISIBLE
                tvAltitude.visibility = View.VISIBLE

                tvLatitude.text = "Latitud: ${formatDouble(uiState.latitude)}"
                tvLongitude.text = "Longitud: ${formatDouble(uiState.longitude)}"
                tvAccuracy.text = "Precisión: ${formatFloat(uiState.accuracy)} m"
                tvSpeed.text = "Velocidad: ${formatFloat(uiState.speed)} m/s"
                tvAltitude.text = "Altitud: ${formatDouble(uiState.altitude)} m"
                tvMovementLabel.text = "Movimiento: ${uiState.movementLabel}"
            }

            ViewMode.PROCESSED -> {
                tvLatitude.visibility = View.GONE
                tvLongitude.visibility = View.GONE
                tvAltitude.visibility = View.GONE

                tvAccuracy.text = "Precisión: ${formatFloat(uiState.accuracy)} m"
                tvSpeed.text = "Velocidad: ${formatFloat(uiState.speed)} m/s"
                tvMovementLabel.text = "Movimiento: ${uiState.movementLabel}"
            }
        }

        tvTimestamp.text = "Última lectura: ${formatTimestamp(uiState.lastUpdateTimestamp)}"
    }

    private fun calculateMovementLabel(speed: Float): String {
        return when {
            speed < 0.3f -> "Quieto"
            speed < 1.5f -> "Desplazamiento leve"
            else -> "En movimiento"
        }
    }

    private fun formatFloat(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}
