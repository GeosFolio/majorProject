package com.example.saferhike.composables

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.HikeListViewModel
import com.example.saferhike.viewModels.HikeReq

@Composable
fun HikeListScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
               apiService: ApiService, hikeListViewModel: HikeListViewModel) {
    val currentUser = authViewModel.currentUser
    val uid = currentUser?.uid
    val hikes = remember { mutableStateOf<List<HikeReq>?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uid) {
        uid?.let {
            hikeListViewModel.getUserHikes(apiService.apiService, uid,
                onSuccess = { hikeList ->
                    hikes.value = hikeList
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                })
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        hikes.value?.let {
            items(it) {
                ExpandableHikeCard(hike = it, navController = navController)
            }
        }
    }
}

@Composable
fun ExpandableHikeCard(hike: HikeReq, navController: NavController) {
    var isExpanded by remember { mutableStateOf(false) }

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
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // If the card is expanded, show additional details and buttons
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // Display hike details
                Text(text = "Supplies: ${hike.supplies}")
                Text(text = "Last Completion Time: ${hike.expectedReturnTime}")

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons (edit, start, delete)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        // Navigate to edit hike screen
                        //navController.navigate("editHike/${hike.uid}")
                    }) {
                        Text(text = "Edit")
                    }

                    Button(onClick = {
                        // Navigate to tracking screen
                        //navController.navigate("trackHike/${hike.uid}")
                    }) {
                        Text(text = "Start Hike")
                    }

                    Button(onClick = {
                        // Handle delete action
                        // Call API to delete hike or trigger delete logic here
                    }) {
                        Text(text = "Delete")
                    }
                }
            }
        }
    }
}