package com.example.saferhike.screens

import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.HikeCreationViewModel
import com.example.saferhike.viewModels.HikeCreationViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHikeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    apiService: ApiService,
    hikeJson: String?,
    viewModel: HikeCreationViewModel = viewModel(factory = HikeCreationViewModelFactory(authViewModel.currentUser?.uid ?: "Uid Missing", hikeJson))
) {
    var locationError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var clickedPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var showDialog by remember { mutableStateOf(false) }
    var markerTitle by remember { mutableStateOf("") }
    var markerDescription by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedMarkerPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var result by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 10f)
    }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
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
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (permissionGranted) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        getCurrentLocation(client) { resultLocation, error ->
            resultLocation?.let {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(it.latitude, it.longitude), 10f
                )
                viewModel.updateHikeDetails(lat = it.latitude, lng = it.longitude)
            }
            locationError = error
        }

        val hikeState by viewModel.hikeReq.collectAsState()
        Scaffold (
            topBar = {
                TopAppBar(
                    title = { Text("New Hike") },
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = hikeState.name,
                    onValueChange = { viewModel.updateHikeDetails(name = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Hike Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                GoogleMap(
                    modifier = Modifier.height(360.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = permissionGranted,
                        mapType = MapType.HYBRID),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = { latLng ->
                        clickedPosition = latLng
                        showDialog = true
                    }
                ) {
                    hikeState.markers.forEach { marker ->
                        Marker(
                            state = MarkerState(LatLng(marker.lat, marker.lng)),
                            title = marker.title,
                            snippet = marker.description,
                            onInfoWindowClick = {
                                selectedMarkerPosition = LatLng(marker.lat, marker.lng)
                                showDeleteDialog = true
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hikeState.supplies,
                    onValueChange = { viewModel.updateHikeDetails(supplies = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Supplies") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveHike(
                            apiService = apiService,
                            onSuccess = { navController.popBackStack() },
                            onError = { e ->
                                Toast.makeText(context, e, Toast.LENGTH_LONG).show()
                                result = e
                            }
                        )
                    }
                ) {
                    Text(text = "Save Hike")
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = result)
            }
        }

        if (showDialog) {
            MarkerDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, description ->
                    viewModel.addMarker(clickedPosition, title, description)
                    showDialog = false
                },
                title = markerTitle,
                onTitleChange = { markerTitle = it },
                description = markerDescription,
                onDescriptionChange = { markerDescription = it }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    Button(onClick = {
                        viewModel.removeMarker(selectedMarkerPosition)
                        showDeleteDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Delete Marker") },
                text = { Text("Are you sure you want to delete this marker?") }
            )
        }
    } else {
        Text("Location access denied. Please grant location access.")
    }
}

fun getCurrentLocation(
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


@Composable
fun MarkerDialog (
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
   AlertDialog(
       onDismissRequest = { onDismiss() },
       confirmButton = {
           Button( onClick = { onConfirm(title, description) }) {
               Text(text = "Add")
           }
                       },
       dismissButton = {
           Button( onClick = { onDismiss() }) {
               Text(text = "Cancel")
           }
       },
       title = { Text(text = "Add Marker") },
       text = {
           Column {
               OutlinedTextField(
                   value = title,
                   onValueChange = onTitleChange,
                   label = { Text(text = "Title") },
                   modifier = Modifier.fillMaxWidth()
               )
               OutlinedTextField(
                   value = description,
                   onValueChange = onDescriptionChange,
                   label = { Text(text = "Description") },
                   modifier = Modifier.fillMaxWidth()
               )
           }
       }
   )
}