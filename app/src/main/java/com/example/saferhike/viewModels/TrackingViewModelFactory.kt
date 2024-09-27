package com.example.saferhike.viewModels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.saferhike.api.ApiService

class TrackingViewModelFactory(
    private val application: Application,
    private val hikeJson: String,
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackingViewModel::class.java)) {
            return TrackingViewModel(application, hikeJson, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
