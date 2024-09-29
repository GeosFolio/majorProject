package com.example.saferhike.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class HikeCreationViewModelFactory(
    private val uid: String,
    private val hikeJson: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HikeCreationViewModel::class.java)) {
            return HikeCreationViewModel(uid, hikeJson) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}
