package com.randomadjective.prototipodatalayer.sensors.providers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.randomadjective.prototipodatalayer.sensors.models.SensorStatus

class LocationSensorProvider(
    private val context: Context,
    private val onStatusChanged: (SensorStatus) -> Unit,
    private val onPermissionChanged: (Boolean) -> Unit,
    private val onLocationChanged: (
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float,
        altitude: Double,
        timestamp: Long
    ) -> Unit
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private var isListening = false

    private val locationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location = result.lastLocation ?: return

            onLocationChanged(
                location.latitude,
                location.longitude,
                location.accuracy,
                location.speed,
                location.altitude,
                System.currentTimeMillis()
            )
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val hasPermission = hasLocationPermission()
        onPermissionChanged(hasPermission)

        if (!hasPermission) {
            onStatusChanged(SensorStatus.ERROR)
            Log.w(TAG, "Permiso de ubicación no concedido.")
            return
        }

        if (isListening) return

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isListening = true
            onStatusChanged(SensorStatus.ACTIVE)
            Log.i(TAG, "LocationSensorProvider iniciado.")
        } catch (e: Exception) {
            onStatusChanged(SensorStatus.ERROR)
            Log.e(TAG, "Error iniciando ubicación: ${e.message}", e)
        }
    }

    fun stop() {
        if (!isListening) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isListening = false
        onStatusChanged(SensorStatus.INACTIVE)
        Log.i(TAG, "LocationSensorProvider detenido.")
    }

    companion object {
        private const val TAG = "LocationProvider"
    }
}
