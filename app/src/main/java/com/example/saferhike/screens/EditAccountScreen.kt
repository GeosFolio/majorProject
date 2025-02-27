package com.example.saferhike.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.EmergencyContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val userData = authViewModel.userData
    var fName by remember { mutableStateOf(userData.fName) }
    var lName by remember { mutableStateOf(userData.lName) }
    val emergencyContacts = remember { SnapshotStateList<EmergencyContact>() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        emergencyContacts.addAll(userData.emergencyContacts)
    }

    Scaffold (
        topBar = {
            TopAppBar(title = { Text("Edit Account") },
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
                    .padding(horizontal = 16.dp)
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Emergency Contacts", fontSize = 18.sp)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(emergencyContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = contact.email,
                                onValueChange = { newEmail ->
                                    val index = emergencyContacts.indexOf(contact)
                                    if (index != -1) {
                                        emergencyContacts[index] = contact.copy(email = newEmail)
                                    }
                                },
                                label = { Text(text = "Email") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = contact.phoneNumber,
                                onValueChange = { newPhone ->
                                    val index = emergencyContacts.indexOf(contact)
                                    if (index != -1) {
                                        emergencyContacts[index] = contact.copy(phoneNumber = newPhone)
                                    }
                                },
                                label = { Text(text = "Phone Number") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (emergencyContacts.size > 1) {
                                Button(
                                    onClick = {
                                        emergencyContacts.remove(contact)
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(text = "-")
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                emergencyContacts.add(EmergencyContact("", ""))
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(text = "+ Add Contact")
                        }
                    }
                }
                Button(
                    onClick = {
                        if (validateContacts(emergencyContacts)) {
                            authViewModel.userData.fName = fName
                            authViewModel.userData.lName = lName
                            authViewModel.userData.emergencyContacts = emergencyContacts.toList()
                            authViewModel.updateUser(authViewModel.userData, apiService)
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Need at least 1 email or phone number",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text(text = "Save Changes")
                }
            }
    }
}