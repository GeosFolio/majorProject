package com.example.saferhike.viewModels

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiService
import com.example.saferhike.tracking.LocationCallback
import com.example.saferhike.tracking.LocationService
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TrackingViewModel(application: Application, hikeJson: String, val apiService: ApiService)
    : AndroidViewModel(application) {
    val permissionGranted = mutableStateOf(false)
    val traveledPath = mutableStateOf<List<LatLng>>(emptyList())
    private val gson = Gson()
    val hike = mutableStateOf<HikeReq>(gson.fromJson(hikeJson, HikeReq::class.java))
    private var locationService: WeakReference<LocationService?> = WeakReference(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as LocationService.LocationBinder).getService()
            locationService = WeakReference(service)
            locationService.get()?.registerCallback(object : LocationCallback {
                override fun onLocationReceived(hikeReq: HikeReq, location: LatLng) {
                    hike.value = hikeReq
                    traveledPath.value = hikeReq.traveledPath
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService.clear()
        }
    }

    fun checkPermissions(context: Context) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        permissionGranted.value = granted
    }

    fun bindService(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        context.unbindService(serviceConnection)
    }

    fun startHikeTracking(context: Context, hikeReq: HikeReq) {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra("HIKE", hikeReq)
        }
        context.startForegroundService(intent)
    }

    fun stopHikeTracking(context: Context) {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        context.stopService(intent)
        viewModelScope.launch {
            hike.value.let {
                Log.d("LocationService", "not in progress, completed true, updating hike")
                it.inProgress = false
                it.completed = true
                val encryptedHikeReq = apiService.encryptHikeReq(it)
                try {
                    apiService.apiService.updateHike(encryptedHikeReq)
                } catch (e: Exception){
                    Log.d("LocationService", "Failed to connect. Trying again later.")
                }
            }
        }
    }

}
