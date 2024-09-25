package com.example.saferhike.navigators

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.composables.HikeListScreen
import com.example.saferhike.composables.NewHikeScreen
import com.example.saferhike.composables.HomeScreen
import com.example.saferhike.composables.LoginScreen
import com.example.saferhike.composables.EditAccountScreen
import com.example.saferhike.composables.SignupScreen
import com.example.saferhike.composables.TrackingScreen

@Composable
fun AppNavigator(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val apiService = ApiService()
    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login") {
            // Done
            LoginScreen(navController, authViewModel, apiService)
        }
        composable("signup") {
            // Done
            SignupScreen(navController, authViewModel, apiService)
        }
        composable("home") {
            // Done
            HomeScreen(navController, authViewModel, apiService)
        }
        composable("newHike/{hikeJson}") {
            val hikeJson = it.arguments?.getString("hikeJson")
            // Done
            NewHikeScreen(navController, authViewModel, apiService, hikeJson)
        }
        composable("newHike") {
            NewHikeScreen(navController, authViewModel, apiService, null)
        }
        composable("hikes") {
            // Done
            HikeListScreen(navController, authViewModel, apiService)
        }
        composable("trackHike/{hikeJson}") {
            // Done
            val hikeJson = it.arguments?.getString("hikeJson")
            TrackingScreen(navController, hikeJson)
        }
        composable("edit") {
            // Done
            EditAccountScreen(navController, authViewModel, apiService)
        }
    })
}