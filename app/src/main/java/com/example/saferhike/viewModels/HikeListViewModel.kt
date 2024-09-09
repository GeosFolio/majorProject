package com.example.saferhike.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HikeListViewModel() : ViewModel() {
    private val _hikes = MutableStateFlow<List<HikeReq>>(emptyList())
    fun getUserHikes(
        apiService: ApiRoutes,
        uid: String,
        onSuccess: (List<HikeReq>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_hikes.value.isEmpty()) {
            viewModelScope.launch {
                try {
                    val response = apiService.getUserHikes(uid)
                    if (response.isSuccessful) {
                        val hikes = response.body()
                        if (hikes != null) {
                            _hikes.value = hikes
                            onSuccess(hikes)
                        } else {
                            onError("No hikes found for user $uid")
                        }
                    } else {
                        onError("Failed to fetch hikes: ${response.code()} : ${response.message()}")
                    }
                } catch (e: HttpException) {
                    onError("HTTP error: ${e.message}")
                } catch (e: IOException) {
                    onError("Network error: ${e.message}")
                } catch (e: Exception) {
                    onError("Unexpected error: ${e.message}")
                }
            }
        } else {
            onSuccess(_hikes.value)
        }

    }
}