package com.example.saferhike.composables

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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.EmergencyContact
import com.example.saferhike.viewModels.UserReq

@Composable
fun EditAccountScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val userData = authViewModel.userData
    var fName by remember { mutableStateOf(userData.fName) }
    var lName by remember { mutableStateOf(userData.lName) }
    // Use SnapshotStateList for emergency contacts
    val emergencyContacts = remember { SnapshotStateList<EmergencyContact>() }
    LaunchedEffect(Unit) {
        emergencyContacts.addAll(userData.emergencyContacts)
    }

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
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Emergency Contacts", fontSize = 18.sp)
        // Use LazyColumn for emergency contacts
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally// Take remaining space
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
                    // Remove contact button
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
                // Add contact button
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                authViewModel.userData.fName = fName
                authViewModel.userData.lName = lName
                authViewModel.userData.emergencyContacts = emergencyContacts.toList()
                authViewModel.updateUser(authViewModel.userData, apiService)
                navController.navigate("home")
            }
        ) {
            Text(text = "Save Changes")
        }
    }
}