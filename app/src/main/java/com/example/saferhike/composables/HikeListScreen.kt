package com.example.saferhike.composables

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.HikeListViewModel
import com.example.saferhike.viewModels.HikeReq
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeListScreen(navController: NavController, authViewModel: AuthViewModel,
               apiService: ApiService, hikeListViewModel: HikeListViewModel = HikeListViewModel()
) {
    Log.d("HikeListScreen", "Creating Hike List Screen")
    val currentUser = authViewModel.currentUser
    val uid = currentUser?.uid
    val hikes = remember { mutableStateOf<List<HikeReq>?>(null) }
    val context = LocalContext.current
    val inProgress = remember { mutableStateOf<HikeReq?>(null) }

    LaunchedEffect(uid) {
        uid?.let {
            hikeListViewModel.getUserHikes(apiService, uid,
                onSuccess = { hikeList ->
                    hikes.value = hikeList
                    Log.d("HikeListScreen", "Received List: $hikeList")
                    inProgress.value = hikeList.find { it.inProgress }
                    Log.d("HikeListScreen", "Hike In Progress: ${inProgress.value}")
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hike List") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate back to the homepage
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
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                hikes.value?.let { hikeList ->
                    items(hikeList) { hike ->
                        Log.d("HikeListScreen", "Creating Expandable Hike Card with: $hike")
                        ExpandableHikeCard(
                            hike,
                            navController,
                            hikeListViewModel,
                            apiService,
                            context,
                            inProgress.value,
                            onDelete = { h ->
                                hikes.value = hikes.value?.filterNot { t -> t.pid == h.pid }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ExpandableHikeCard(hike: HikeReq, navController: NavController,
                       hikeListViewModel: HikeListViewModel, apiService: ApiService,
                       context: Context, inProgressHike: HikeReq?, onDelete: (h: HikeReq) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    val gson = Gson()
    Log.d("HikeListScreen", "Making Hike Card With: $hike : $inProgressHike")
    // The card UI
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(), // Smooth transition when expanding/collapsing
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row to display the hike name and the expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hike.name,
                    modifier = Modifier.weight(1f) // Pushes the icon to the right
                )

                // Button to expand/collapse the card
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // If the card is expanded, show additional details and buttons
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Supplies: ${hike.supplies}")
                Text(text = "Last Completion Time: ${hike.expectedReturnTime}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        val hikeJson = gson.toJson(hike)
                        navController.navigate("newHike/$hikeJson")
                    }) {
                        Text(text = "Edit")
                    }
                    Button(onClick = {
                        if (inProgressHike != null && inProgressHike.pid == hike.pid) {
                            hikeListViewModel.getHikePath(apiService, hike.pid,
                                hike.uid,
                                onSuccess = { path ->
                                    hike.traveledPath = path
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                })
                            val hikeJson = gson.toJson(hike)
                            navController.navigate("trackHike/$hikeJson")
                        } else {
                            showPrompt = true
                        }
                    },
                        enabled = inProgressHike == null || inProgressHike.pid == hike.pid ) {
                        if (inProgressHike != null && inProgressHike.pid == hike.pid) {
                            Text("Continue Hike")
                        } else {
                            Text("Start Hike")
                        }
                    }
                    Button(onClick = {
                        // Handle delete action
                        // Call API to delete hike or trigger delete logic here
                        hikeListViewModel.delete(apiService.apiService, hike,
                            onSuccess = {
                                Toast.makeText(context, "Hike Deleted Successfully",
                                    Toast.LENGTH_LONG).show()
                            },
                            onError = {
                                Toast.makeText(context, "Failed to delete hike: $it",
                                    Toast.LENGTH_LONG).show()
                            })
                        onDelete(hike)
                    },
                        enabled = inProgressHike == null || inProgressHike.pid != hike.pid) {
                        Text(text = "Delete")
                    }
                }
            }
        }
        if (showPrompt) {
            DurationDialog(
                onConfirm = { hours, minutes ->
                    hike.expectedReturnTime = "$hours:$minutes"
                    hikeListViewModel.startHike(hike, apiService,
                        onSuccess = {
                            val hikeJson = gson.toJson(hike)
                            navController.navigate("trackHike/$hikeJson")
                        },
                        onError = {
                            Toast.makeText(context, "Failed to start hike: $it",
                                Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onDismiss = {
                    showPrompt = false
                }
            )
        }

    }
}

@Composable
fun DurationDialog(
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Expected Hike Duration") },
        text = {
            Column {
                TextField(
                    value = hours.toString(),
                    onValueChange = {
                    hours = it.toIntOrNull() ?: 0
                },
                    label = { Text("Hours") }
                )
                TextField(
                    value = minutes.toString(),
                    onValueChange = { minutes = it.toIntOrNull() ?: 0 },
                    label = { Text("Minutes")}
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(hours, minutes)
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}