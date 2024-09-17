package com.example.saferhike.composables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthState
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.UserReq
import kotlinx.coroutines.launch

@Composable
fun EditAccountScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
                      apiService: ApiService) {
    val userData = authViewModel.userData
    var fName by remember { mutableStateOf(userData.fName) }
    var lName by remember { mutableStateOf(userData.lName) }
    var emergencyContacts by remember { mutableStateOf(userData
        .emergencyContacts
        .joinToString(",\n")) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Edit Account", fontSize = 32.sp)

        OutlinedTextField(
            value = fName,
            onValueChange = { fName = it },
            label = { Text("First Name") }
        )

        OutlinedTextField(
            value = lName,
            onValueChange = { lName = it },
            label = { Text("Last Name") }
        )

        OutlinedTextField(
            value = emergencyContacts,
            onValueChange = { emergencyContacts = it },
            label = { Text("Emergency Contacts (separate by commas)") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val updatedUser = UserReq(
                    uid = authViewModel.currentUser?.uid ?: "",
                    fName = fName,
                    lName = lName,
                    emergencyContacts = emergencyContacts.split(",").map { it.trim() }
                )
                authViewModel.updateUser(updatedUser, apiService.apiService)
                navController.navigate("home")
            }
        ) {
            Text(text = "Save Changes")
        }
    }
}