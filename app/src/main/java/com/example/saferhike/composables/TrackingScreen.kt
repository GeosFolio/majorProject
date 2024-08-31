package com.example.saferhike.composables

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.authentication.AuthViewModel
import com.example.saferhike.tracking.LocationService

@Composable
fun TrackingScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
                   apiService: ApiService) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

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
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
    if (permissionGranted) {
        Column (
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Testing location tracking")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    ContextCompat.startForegroundService(context, this)
                }
            }) {
                Text(text = "Start")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                    ContextCompat.startForegroundService(context, this)
                }
            }) {
                Text(text = "Stop testing change")
            }
        }
    } else {
        Text("Location access denied. Please grant location access.")
    }
}