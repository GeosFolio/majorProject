package com.example.saferhike.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthState
import com.example.saferhike.viewModels.AuthViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel,
               apiService: ApiService) {
    val authState = authViewModel.authState.observeAsState()
    val user: FirebaseUser? = authViewModel.currentUser
    val userData = authViewModel.userData

    val distance by remember {
        mutableIntStateOf(0)
    }
    val hikes by remember {
        mutableIntStateOf(0)
    }
    val elevation by remember {
        mutableIntStateOf(0)
    }
    val first by remember {
        mutableStateOf(userData.fName)
    }
    LaunchedEffect(authState.value) {
        when(authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Page", fontSize = 32.sp)
        Text(text = user?.uid ?: "Uid Missing")
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Welcome Back $first", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                navController.navigate("newHike")
            }) {
                Text(text = "New Hike")
            }
            Button(onClick = {
                navController.navigate("hikes")
            }) {
                Text(text = "View Past Hikes")
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
        Text(text = "Total Hikes Completed: $hikes", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "Distance Travelled: $distance km", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "Elevation Gained: $elevation km", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(64.dp))
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { navController.navigate("edit") }) {
                Text(text = "Edit Account")
            }
            Spacer(modifier = Modifier.width(18.dp))
            Button(onClick = {authViewModel.signout()}) {
                Text(text = "Sign-out")
            }
        }
        
    }
}