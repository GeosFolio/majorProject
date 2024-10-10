package com.example.saferhike.screens

import android.Manifest
import android.app.Application
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.TrackingViewModel
import com.example.saferhike.viewModels.TrackingViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    navController: NavController,
    hikeJson: String,
    apiService: ApiService,
    viewModel: TrackingViewModel = viewModel(factory = TrackingViewModelFactory(LocalContext.current.applicationContext as Application, hikeJson, apiService))
) {
    val context = LocalContext.current
    val permissionGranted by viewModel.permissionGranted
    val traveledPath by viewModel.traveledPath
    val hike = viewModel.hike.value

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(hike.lat, hike.lng), 12f)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.permissionGranted.value =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
        if (!permissionGranted) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    if (permissionGranted) {
        LaunchedEffect(Unit) {
            Log.d("TrackingScreen", "Start Hike Tracking sent")
            viewModel.startHikeTracking(context, hike)
        }
        DisposableEffect(Unit) {
            viewModel.bindService(context)
            onDispose {
                viewModel.unbindService(context)
            }
        }
        LaunchedEffect(traveledPath) {
            if (traveledPath.isNotEmpty()) {
                val latestLocation = traveledPath.last()
                cameraPositionState.animate(
                    CameraUpdateFactory
                        .newCameraPosition(
                            CameraPosition.fromLatLngZoom(latestLocation, 15f)
                        )
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Current Hike: ${hike.name}") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.HYBRID),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    hike.markers.forEach { marker ->
                        Marker(
                            state = MarkerState(position = LatLng(marker.lat, marker.lng)),
                            title = marker.title,
                            snippet = marker.description
                        )
                    }
                    Polyline(
                        points = hike.markers.map { LatLng(it.lat, it.lng) },
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
                    viewModel.stopHikeTracking(context)
                    navController.popBackStack()
                    navController.popBackStack()
                }) {
                    Text(text = "Complete Hike")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Text("Location access denied. Please grant location access.")
    }
}
