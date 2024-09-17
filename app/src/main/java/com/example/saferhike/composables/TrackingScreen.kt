package com.example.saferhike.composables

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.tracking.LocationService
import com.example.saferhike.viewModels.HikeReq
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TrackingScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
                   hikeJson: String?) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var traveledPath by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val gson = Gson()
    var hike = hikeJson?.let { gson.fromJson(it, HikeReq::class.java) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(hike?.lat?: 0.0, hike?.lng?: 0.0), 12f)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
        }
    }
    if (permissionGranted) {
        hike?.let {
            if (!it.inProgress) {
                Log.d("TrackingScreen", "Starting Hike Tracking of Hike: $it")
                startHikeTracking(context, it)
            }
        }
        val serviceConnection = rememberUpdatedState<ServiceConnection>(object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val locationService = (binder as LocationService.LocationBinder).getService()
                locationService.registerCallback(object : LocationCallback {
                    override fun onLocationReceived(hikeReq: HikeReq, location: LatLng) {
                        hike = hikeReq
                        traveledPath = hikeReq.traveledPath
                        CoroutineScope(Dispatchers.Main).launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLng(location))
                        }
                    }
                })
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Handle disconnection
            }
        })
        val intent = Intent(context, LocationService::class.java)
        context.bindService(intent, serviceConnection.value, Context.BIND_AUTO_CREATE)
        DisposableEffect(Unit) {
            onDispose {
                context.unbindService(serviceConnection.value)
            }
        }
        Column (
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Current Hike: ${hike?.name?: "No hike found"}")
            Spacer(modifier = Modifier.height(16.dp))
            GoogleMap(
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = permissionGranted),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                hike?.markers?.forEach { marker ->
                    Marker(
                        state = MarkerState(position = LatLng(marker.lat, marker.lng)),
                        title = marker.title,
                        snippet = marker.description
                    )
                }
                Polyline(
                    points = hike?.markers?.map { LatLng(it.lat, it.lng) }?: emptyList(),
                    color = Color.Blue,
                    width = 5f
                )
                Polyline(
                    points = traveledPath,
                    color = Color.Green,
                    width = 5f
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                stopHikeTracking(context)
            }) {
                Text(text = "Complete Hike")
            }
        }
    } else {
        Text("Location access denied. Please grant location access.")
    }
}

fun startHikeTracking(context: Context, hikeReq: HikeReq) {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_START
        putExtra("HIKE", hikeReq)
    }
    context.startForegroundService(intent)
}

fun stopHikeTracking(context: Context) {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_STOP
    }
    context.startForegroundService(intent)
}