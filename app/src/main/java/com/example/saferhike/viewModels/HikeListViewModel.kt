package com.example.saferhike.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
import com.example.saferhike.api.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HikeListViewModel() : ViewModel() {
    private val _hikes = MutableStateFlow<List<HikeReq>>(emptyList())
    fun getUserHikes(
        apiService: ApiService,
        uid: String,
        onSuccess: (List<HikeReq>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.apiService.getUserHikes(uid)
                if (response.isSuccessful) {
                    val hikes = response.body()
                    val decryptedHikes = emptyList<HikeReq>().toMutableList()
                    Log.d("HikeListViewModel", "Trying to decrypt hike list")
                    hikes?.forEach {
                        decryptedHikes += apiService.decryptHikeReq(it, uid)
                    }
                    Log.d("HikeListViewModel", "Done decrypting hike list")
                    _hikes.value = decryptedHikes
                    onSuccess(_hikes.value)
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
    }
    fun delete(apiService: ApiRoutes, hikeReq: HikeReq,
               onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteHike(hikeReq.pid)
                if (response.isSuccessful) {
                    _hikes.value = _hikes.value.filterNot { it.pid == hikeReq.pid }
                    onSuccess()
                } else {
                    onError("Failed to delete hike: ${response.code()} : ${response.message()}")
                }
            } catch (e: HttpException) {
                onError("HTTP error: ${e.message}")
            } catch (e: IOException) {
                onError("Network error: ${e.message}")
            } catch (e: Exception) {
                onError("Unexpected error: ${e.message}")
            }
        }
    }

    fun startHike(hikeReq: HikeReq, apiService: ApiService,
                  onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val encryptedHikeReq = apiService.encryptHikeReq(hikeReq)
                val response = apiService.apiService.startHike(encryptedHikeReq)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Something wrong with flask :${response.message()}:${response.code()}")
                }
            } catch (e: HttpException) {
                onError("HTTP error: ${e.message}")
            } catch (e: IOException) {
                onError("Network error: ${e.message}")
            } catch (e: Exception) {
                onError("Unexpected Error: ${e.message}")
            }
        }
    }
}