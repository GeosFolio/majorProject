package com.example.saferhike.viewModels

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiService
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class HikeCreationViewModel(uid: String, hikeJson: String?) : ViewModel() {
    private val gson = Gson()
    private val _hikeReq = MutableStateFlow(
        hikeJson?.let { gson.fromJson(it, HikeReq::class.java) } ?: HikeReq(
            pid = 0,
            uid = uid,
            name = "",
            supplies = "",
            lat = 0.0,
            lng = 0.0,
            duration = "",
            markers = emptyList(),
            traveledPath = emptyList()
        )
    )

    val hikeReq: StateFlow<HikeReq> get() = _hikeReq

    fun updateHikeDetails(
        name: String? = null,
        supplies: String? = null,
        expectedReturnTime: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        _hikeReq.value = _hikeReq.value.copy(
            name = name ?: _hikeReq.value.name,
            supplies = supplies ?: _hikeReq.value.supplies,
            duration = expectedReturnTime ?: _hikeReq.value.duration,
            lat = lat ?: _hikeReq.value.lat,
            lng = lng ?: _hikeReq.value.lng
        )
    }

    fun addMarker(position: LatLng, t: String, d: String) {
        val newMarker = HikeMarker(
            lat = position.latitude,
            lng = position.longitude,
            title = t,
            description = d
        )
        _hikeReq.value = _hikeReq.value.copy(
            markers = _hikeReq.value.markers + newMarker
        )
    }

    fun removeMarker(position: LatLng) {
        _hikeReq.value = _hikeReq.value.copy(
            markers = _hikeReq.value.markers.filterNot {
                it.lat == position.latitude && it.lng == position.longitude
            }
        )
    }

    fun saveHike(apiService: ApiService, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: Response<Void> = if (_hikeReq.value.pid != 0) {
                    val encryptedHikeReq = apiService.encryptHikeReq(_hikeReq.value)
                    apiService.apiService.updateHike(encryptedHikeReq)
                } else {
                    val encryptedHikeReq = apiService.encryptHikeReq(_hikeReq.value)
                    apiService.apiService.postHike(encryptedHikeReq)
                }
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Failed to save hike: ${response.code()} : ${response.message()}")
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
}

@Parcelize
data class HikeReq(
    val pid: Int,
    val uid: String,
    val name: String,
    val supplies: String,
    val lat: Double,
    val lng: Double,
    var duration: String,
    val markers: List<HikeMarker>,
    var traveledPath: List<LatLng>,
    var completed: Boolean = false,
    var inProgress: Boolean = false
) : Parcelable

@Parcelize
data class HikeMarker(
    val lat: Double,
    val lng: Double,
    val title: String,
    val description: String
) : Parcelable
