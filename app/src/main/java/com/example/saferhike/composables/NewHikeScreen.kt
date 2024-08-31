package com.example.saferhike.composables

import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.authentication.AuthViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NewHikeScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
               apiService: ApiService, viewModel: HikeCreationViewModel = remember {
        HikeCreationViewModel()
    }) {
    var location by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var mapClickMode by remember { mutableStateOf(MapClickMode.NONE) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissionGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else {
            launcher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
    if (permissionGranted) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        getCurrentLocation(client) { resultLocation, error ->
            location = resultLocation
            locationError = error
        }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(location?.latitude?: 0.0,
                location?.longitude?: 0.0), 10f)
        }
        val markers = viewModel.hike.markers.collectAsState()
        Scaffold (
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    viewModel.addMarker(cameraPositionState.position.target)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Marker")
                }
            }
        ) {
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicTextField(
                    value = viewModel.hike.name.value,
                    onValueChange = { viewModel.hike.name.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                    ) { innerTextField ->
                    if (viewModel.hike.name.value.isEmpty()) {
                        Text("Hike Name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = viewModel.hike.supplies.value,
                    onValueChange = { viewModel.hike.supplies.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                ) { innerTextField ->
                    if (viewModel.hike.supplies.value.isEmpty()) {
                        Text("Supplies", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = viewModel.hike.expectedReturnTime.value,
                    onValueChange = { viewModel.hike.expectedReturnTime.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                ) { innerTextField ->
                    if (viewModel.hike.expectedReturnTime.value.isEmpty()) {
                        Text("Expected Return Time", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(8.dp))
                GoogleMap (
                    modifier = Modifier.fillMaxWidth(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = permissionGranted),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true)
                ) {
                    markers.value.forEach { marker ->
                        Marker(
                            state = marker.state,
                            title = marker.title,
                            contentDescription = marker.description
                        )
                    }
                }
            }
        }
    } else {
        Text("Location access denied. Please grant location access.")
    }
}

fun getCurrentLocation (
    locationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?, String?) -> Unit
) {
    val cancellationTokenSource = CancellationTokenSource()
    
    locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationReceived(location, null)
            } else {
                onLocationReceived(null, "Failed to retrieve location.")
            }
        }
        .addOnFailureListener { exception ->
            onLocationReceived(null, "Error: ${exception.localizedMessage}")
        }
}

class HikeCreationViewModel : ViewModel() {
    private val _markers = MutableStateFlow<List<HikeMarker>>(emptyList())
    var hike = Hike(
        name = mutableStateOf(""),
        supplies = mutableStateOf(""),
        expectedReturnTime = mutableStateOf(""),
        markers = _markers
    )

    fun addMarker(position: LatLng) {
        val newMarker = HikeMarker(
            state = MarkerState(position),
            title = "Custom Marker",
            description = "Add Details Here"
        )
        _markers.value = _markers.value + newMarker
    }
}

data class Hike(
    var name: MutableState<String>,
    var supplies: MutableState<String>,
    var expectedReturnTime: MutableState<String>,
    var markers: StateFlow<List<HikeMarker>>,
    var totalDistance: Double = 0.0,
    var totalElevationChange: Double = 0.0
)
data class HikeMarker (
    val state: MarkerState,
    var title: String,
    var description: String
)
enum class MapClickMode {
    NONE, ADD_MARKER
}