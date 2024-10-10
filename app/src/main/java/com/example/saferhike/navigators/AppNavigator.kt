package com.example.saferhike.navigators

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.screens.HikeListScreen
import com.example.saferhike.screens.NewHikeScreen
import com.example.saferhike.screens.HomeScreen
import com.example.saferhike.screens.LoginScreen
import com.example.saferhike.screens.EditAccountScreen
import com.example.saferhike.screens.SignupScreen
import com.example.saferhike.screens.TrackingScreen

@Composable
fun AppNavigator(innerPadding: PaddingValues, authViewModel: AuthViewModel) {
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
            HomeScreen(navController, authViewModel)
        }
        composable("newHike/{hikeJson}") {
            val hikeJson = it.arguments?.getString("hikeJson")
            // Done
            NewHikeScreen(navController, authViewModel, apiService, hikeJson)
        }
        composable("newHike") {
            // Done
            NewHikeScreen(navController, authViewModel, apiService, null)
        }
        composable("hikes") {
            // Done
            HikeListScreen(navController, authViewModel, apiService)
        }
        composable("trackHike/{hikeJson}") {
            // Done
            val hikeJson = it.arguments?.getString("hikeJson")?: ""
            TrackingScreen(navController, hikeJson, apiService)
        }
        composable("edit") {
            // Done
            EditAccountScreen(navController, authViewModel, apiService)
        }
    })
}