package com.example.saferhike.navigators

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.AuthViewModel
import com.example.saferhike.composables.AccountScreen
import com.example.saferhike.composables.HikeScreen
import com.example.saferhike.composables.NewHikeScreen
import com.example.saferhike.composables.HomeScreen
import com.example.saferhike.composables.LoginScreen
import com.example.saferhike.composables.OnboardingScreen
import com.example.saferhike.composables.SettingsScreen
import com.example.saferhike.composables.SignupScreen
import com.example.saferhike.composables.TrackingScreen

@Composable
fun AppNavigator(modifier: Modifier = Modifier,authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val apiService = ApiService()
    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login") {
            LoginScreen(modifier, navController, authViewModel)
        }
        composable("signup") {
            SignupScreen(modifier, navController, authViewModel, apiService)
        }
        composable("home") {
            HomeScreen(modifier, navController, authViewModel, apiService)
        }
        composable("newAccount") {
            AccountScreen(modifier, navController, authViewModel, apiService)
        }
        composable("newHike") {
            NewHikeScreen(modifier, navController, authViewModel, apiService)
        }
        composable("hikes") {
            HikeScreen(modifier, navController, authViewModel, apiService)
        }
        composable("trackHike") {
            TrackingScreen(modifier, navController, authViewModel, apiService)
        }
        composable("settings") {
            SettingsScreen(modifier, navController, authViewModel, apiService)
        }
        composable("onboard") {
            OnboardingScreen(modifier, navController, authViewModel, apiService)
        }
    })
}