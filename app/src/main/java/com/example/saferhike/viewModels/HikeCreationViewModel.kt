package com.example.saferhike.viewModels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HikeCreationViewModel(uid: String) : ViewModel() {
    private val _markers = MutableStateFlow<List<HikeMarker>>(emptyList())
    var hike = Hike(
        uid = uid,
        name = mutableStateOf(""),
        supplies = mutableStateOf(""),
        expectedReturnTime = mutableStateOf(""),
        lat = mutableDoubleStateOf(0.0),
        lng = mutableDoubleStateOf(0.0),
        markers = _markers
    )

    fun addMarker(position: LatLng, t: String, d: String) {
        val newMarker = HikeMarker(
            state = MarkerState(position),
            title = t,
            description = d
        )
        _markers.value = _markers.value + newMarker
    }

    fun saveHike(apiService: ApiRoutes,
                 onSuccess: () -> Unit,
                 onError: (String) -> Unit) {
        val hikeData = HikeReq(
            uid = hike.uid,
            name = hike.name.value,
            supplies = hike.supplies.value,
            expectedReturnTime = hike.expectedReturnTime.value,
            lat = hike.lat.value,
            lng = hike.lng.value,
            markers = hike.markers.value.map { marker ->
                HikeMarkerReq(
                    lat = marker.state.position.latitude,
                    lng = marker.state.position.longitude,
                    title = marker.title,
                    description = marker.description
                )
            }
        )
        viewModelScope.launch {
            try {
                val response = apiService.createHike(hikeData)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Failed to create hike: ${response.code()} : ${response.message()}")
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

data class Hike(
    val uid: String,
    var name: MutableState<String>,
    var supplies: MutableState<String>,
    var expectedReturnTime: MutableState<String>,
    var lat: MutableState<Double>,
    var lng: MutableState<Double>,
    var markers: StateFlow<List<HikeMarker>>
)
data class HikeMarker (
    val state: MarkerState,
    var title: String,
    var description: String
)
data class HikeReq(
    val uid: String,
    val name: String,
    val supplies: String,
    var lat: Double,
    var lng: Double,
    val expectedReturnTime: String,
    val markers: List<HikeMarkerReq>
)

data class HikeMarkerReq(
    val lat: Double,
    val lng: Double,
    val title: String,
    val description: String
)