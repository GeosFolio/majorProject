package com.example.saferhike.tracking

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing Location Permission")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if(!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }

            val reqBuilder = LocationRequest.Builder(interval)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            val request = reqBuilder.build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult) {
                    Log.d("DefaultLocationClient", "Sending location result: $res")
                    super.onLocationResult(res)
                    res.locations.lastOrNull()?.let { location ->
                        Log.d("DefaultLocationClient", "V2: Sending location result: $location")
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}