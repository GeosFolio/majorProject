package com.example.saferhike.viewModels

import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
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
    private val h = hikeJson?.let { gson.fromJson(it, HikeReq::class.java) }
    private val _markers = MutableStateFlow<List<HikeMarker>>(emptyList())
    var hike = Hike(
        uid = uid,
        name = mutableStateOf(h?.name?:""),
        supplies = mutableStateOf(h?.supplies?:""),
        expectedReturnTime = mutableStateOf(h?.expectedReturnTime?:""),
        lat = mutableDoubleStateOf(h?.lat?:0.0),
        lng = mutableDoubleStateOf(h?.lng?:0.0),
        markers = _markers
    )

    fun addMarker(position: LatLng, t: String, d: String) {
        val newMarker = HikeMarker(
            lat = position.latitude,
            lng = position.longitude,
            title = t,
            description = d
        )
        _markers.value = _markers.value + newMarker
    }

    fun removeMarker(position: LatLng) {
        _markers.value = _markers.value.filterNot {
            it.lat == position.latitude && it.lng == position.longitude
        }
    }

    fun saveHike(apiService: ApiRoutes,
                 onSuccess: () -> Unit,
                 onError: (String) -> Unit) {
        val hikeData = HikeReq(
            pid = 0,
            uid = hike.uid,
            name = hike.name.value,
            supplies = hike.supplies.value,
            expectedReturnTime = hike.expectedReturnTime.value,
            lat = hike.lat.value,
            lng = hike.lng.value,
            traveledPath = emptyList(),
            markers = hike.markers.value.map { marker ->
                HikeMarker(
                    lat = marker.lat,
                    lng = marker.lng,
                    title = marker.title,
                    description = marker.description
                )
            }
        )
        viewModelScope.launch {
            try {
                val response: Response<Void> = if (h != null) {
                    apiService.updateHike(hikeData)
                } else {
                    apiService.postHike(hikeData)
                }
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
@Parcelize
data class HikeReq(
    val pid: Int,
    val uid: String,
    val name: String,
    val supplies: String,
    val lat: Double,
    val lng: Double,
    val expectedReturnTime: String,
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