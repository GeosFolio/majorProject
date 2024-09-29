package com.example.saferhike.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthState
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.viewModels.EmergencyContact

@Composable
fun SignupScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fName by remember { mutableStateOf("") }
    var lName by remember { mutableStateOf("") }
    val emergencyContacts = remember { SnapshotStateList<EmergencyContact>() }
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> navController.navigate("home")
            is AuthState.Error -> Toast.makeText(
                context,
                (authState.value as AuthState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fixed header section
        Text(text = "SaferHike", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Form fields
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = fName,
            onValueChange = { fName = it },
            label = { Text(text = "First Name") }
        )
        OutlinedTextField(
            value = lName,
            onValueChange = { lName = it },
            label = { Text(text = "Last Name") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Emergency Contacts Section
        Text(text = "Emergency Contacts", fontSize = 20.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

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
        Button(
            onClick = {
                val contactList = emergencyContacts.toList()
                if (validateContacts(contactList)) {
                    authViewModel.signup(email, password, fName, lName, contactList, apiService)
                } else {
                    Toast.makeText(context, "Need at least 1 email or phone number",
                        Toast.LENGTH_LONG).show()
                }
            },
            enabled = authState.value != AuthState.Loading
                    || authState.value != AuthState.Authenticated
        ) {
            Text(text = "Signup")
        }
        TextButton(
            onClick = {
                navController.navigate("login")
            }
        ) {
            Text(text = "Already have an account?")
        }
    }
}

fun validateContacts(contacts: List<EmergencyContact>): Boolean {
    return contacts.all { it.email != "" || it.phoneNumber != "" }
}