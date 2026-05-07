package com.randomadjective.prototipodatalayer.sensors.fragments.gameplay

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.randomadjective.prototipodatalayer.R
import com.randomadjective.prototipodatalayer.sensors.models.RadarPOI
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus
import com.randomadjective.prototipodatalayer.sensors.providers.LocationSensorProvider
import com.randomadjective.prototipodatalayer.sensors.providers.OrientationSensorProvider
import java.util.Locale

class LocationSensorFragmentGameplay : Fragment(R.layout.fragment_gameplay_sensor_location) {

    private lateinit var radarView: RadarView
    private lateinit var tvRadarStatus: TextView
    private lateinit var tvRadarZoom: TextView
    private lateinit var tvRadarHeading: TextView

    private lateinit var locationProvider: LocationSensorProvider
    private lateinit var orientationProvider: OrientationSensorProvider

    private var hasPermission: Boolean = false
    private var currentStatus: SensorStatus = SensorStatus.INACTIVE

    private val manualPOIs = listOf(
        RadarPOI(
            id = "poi_01",
            name = "POI 1",
            latitude = -35.4046086,
            longitude = -71.6319017
        ),
        RadarPOI(
            id = "poi_02",
            name = "POI 2",
            latitude = -35.4041761,
            longitude = -71.6342183
        ),
        RadarPOI(
            id = "poi_03",
            name = "POI 3",
            latitude =  -35.4041991,
            longitude = -71.6320264
        )
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            hasPermission = granted

            if (granted) {
                updateStatusText("Buscando ubicación...")
                locationProvider.start()
            } else {
                currentStatus = SensorStatus.ERROR
                updateStatusText("Permiso de ubicación denegado")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        radarView = view.findViewById(R.id.radarView)
        tvRadarStatus = view.findViewById(R.id.tvRadarStatus)
        tvRadarZoom = view.findViewById(R.id.tvRadarZoom)
        tvRadarHeading = view.findViewById(R.id.tvRadarHeading)

        setupRadarView()
        setupLocationProvider()
        setupOrientationProvider()

        updateStatusText("Buscando ubicación...")
        updateZoomText()
        updateHeadingText(null)
    }

    override fun onResume() {
        super.onResume()

        orientationProvider.start()

        if (hasLocationPermission()) {
            hasPermission = true
            updateStatusText("Buscando ubicación...")
            locationProvider.start()
        } else {
            hasPermission = false
            requestLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()

        if (::locationProvider.isInitialized) {
            locationProvider.stop()
        }

        if (::orientationProvider.isInitialized) {
            orientationProvider.stop()
        }
    }

    private fun setupRadarView() {
        radarView.setPOIs(manualPOIs)

        radarView.setCallbacks(
            onZoomChanged = {
                updateZoomText()
            },
            onStatusChanged = { message ->
                updateStatusText(message)
            },
            onPoiFound = { poi ->
                updateStatusText("POI encontrado: ${poi.name}")

                // Después conectamos esto con Unity/smartphone:
                // sendMessage("POI_FOUND:${poi.id}")
            }
        )
    }

    private fun setupLocationProvider() {
        locationProvider = LocationSensorProvider(
            context = requireContext(),
            onStatusChanged = { status ->
                currentStatus = status

                when (status) {
                    SensorStatus.ACTIVE -> updateStatusText("Radar activo")
                    SensorStatus.INACTIVE -> updateStatusText("Radar inactivo")
                    SensorStatus.ERROR -> updateStatusText("Error de ubicación")
                    SensorStatus.NOT_AVAILABLE -> updateStatusText("Ubicación no disponible")
                }
            },
            onPermissionChanged = { granted ->
                hasPermission = granted

                if (!granted) {
                    updateStatusText("Permiso de ubicación denegado")
                }
            },
            onLocationChanged = { latitude, longitude, accuracy, speed, altitude, timestamp ->
                radarView.updateUserLocation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy
                )

                updateStatusText(
                    "GPS activo | ±${String.format(Locale.US, "%.1f", accuracy)} m"
                )
            }
        )
    }

    private fun setupOrientationProvider() {
        orientationProvider = OrientationSensorProvider(
            context = requireContext(),
            onHeadingChanged = { heading ->
                radarView.updateHeading(heading)
                updateHeadingText(heading)
            },
            onAvailabilityChanged = { available ->
                if (!available) {
                    updateHeadingText(null)
                    updateStatusText("Orientación no disponible")
                }
            }
        )
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

    private fun updateStatusText(message: String) {
        if (!::tvRadarStatus.isInitialized) return

        tvRadarStatus.text = message

        tvRadarStatus.setTextColor(
            when {
                message.contains("encontrado", ignoreCase = true) -> Color.YELLOW
                message.contains("error", ignoreCase = true) -> Color.RED
                message.contains("denegado", ignoreCase = true) -> Color.RED
                message.contains("no disponible", ignoreCase = true) -> Color.RED
                message.contains("activo", ignoreCase = true) -> Color.rgb(185, 255, 185)
                else -> Color.WHITE
            }
        )
    }

    private fun updateZoomText() {
        if (!::tvRadarZoom.isInitialized) return

        tvRadarZoom.text = "Zoom: ${radarView.getCurrentZoom().label}"
    }

    private fun updateHeadingText(heading: Float?) {
        if (!::tvRadarHeading.isInitialized) return

        tvRadarHeading.text = if (heading == null) {
            "Heading: -"
        } else {
            "Heading: ${heading.toInt()}°"
        }
    }
}