package com.example.saferhike.composables

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.HikeCreationViewModel
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
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Calendar

@Composable
fun NewHikeScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
                  apiService: ApiService, viewModel: HikeCreationViewModel = remember {
        HikeCreationViewModel(authViewModel.currentUser?.uid ?: "Uid Missing")
    }) {

    var locationError by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var clickedPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var showDialog by remember { mutableStateOf(false) }
    var markerTitle by remember { mutableStateOf("") }
    var markerDescription by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0,
            0.0), 10f)
    }

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
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(resultLocation?.latitude
                ?: 0.0, resultLocation?.longitude?: 0.0), 10f)
            locationError = error
            viewModel.hike.lat.value = resultLocation?.latitude?: 0.0
            viewModel.hike.lng.value = resultLocation?.longitude?: 0.0
        }
        val markers = viewModel.hike.markers.collectAsState()
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
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
        Spacer(modifier = Modifier.height(16.dp))
        GoogleMap (
            modifier = Modifier.height(360.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            onMapClick = { latLng ->
                clickedPosition = latLng
                showDialog = true
            }
        ) {
            markers.value.forEach { marker ->
                Marker(
                    state = marker.state,
                    title = marker.title,
                    snippet = marker.description
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        BasicTextField(
            value = viewModel.hike.supplies.value,
            onValueChange = { viewModel.hike.supplies.value = it },
            modifier = Modifier.fillMaxWidth(),
        ) { innerTextField ->
            if (viewModel.hike.supplies.value.isEmpty()) {
                Text("Supplies", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            innerTextField()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Expected Return Date: $selectedDate")
        Button(onClick = { showDatePicker = true }) {
            Text("Select Date")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Expected Return Time: $selectedTime")
        Button(onClick = { showTimePicker = true }) {
            Text("Select Time")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button (
            onClick = {
                viewModel.saveHike(
                    apiService = apiService.apiService,
                    onSuccess = {
                        navController.navigate("hikes")
                    },
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
        if (showDialog) {
            MarkerDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, description ->
                            viewModel.addMarker(
                                clickedPosition,
                                title,
                                description
                            )
                    showDialog = false
                },
                title = markerTitle,
                onTitleChange = { markerTitle = it },
                description = markerDescription,
                onDescriptionChange = { markerDescription = it }
            )
        }
        if (showDatePicker) {
            DatePicker { date ->
                selectedDate = date
                showDatePicker = false
                viewModel.hike.expectedReturnTime.value = "$date $selectedTime"
            }
        }

        if (showTimePicker) {
            TimePicker { time ->
                selectedTime = time
                showTimePicker = false
                viewModel.hike.expectedReturnTime.value = "$selectedDate $time"
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

@Composable
fun MarkerDialog (
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
   AlertDialog(onDismissRequest = { /*TODO*/ },
       confirmButton = {
           Button( onClick = { onConfirm(title, description) } ) {
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

@Composable
fun DatePicker(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val date = "${month + 1}/$dayOfMonth/$year"
            onDateSelected(date)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    datePickerDialog.show()
}
@Composable
fun TimePicker(onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val time = String.format("%02d:%02d", hourOfDay, minute)
            onTimeSelected(time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    timePickerDialog.show()
}