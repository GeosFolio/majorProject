package com.example.saferhike.tracking

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.saferhike.R
import com.example.saferhike.api.ApiService
import com.example.saferhike.viewModels.HikeReq
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private val apiService = ApiService()
    private var hikeReq: HikeReq? = null
    private val binder = LocationBinder()
    private val clients = mutableListOf<LocationCallback>()

    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }


    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "on start hit")
        Log.d("LocationService", "${intent?.action}")
        when(intent?.action) {
            ACTION_START -> start(intent)
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    private fun start(intent: Intent?) {
        hikeReq = intent?.getParcelableExtra("HIKE", HikeReq::class.java)
        if (hikeReq == null) {
            Log.d("LocationService", "Hike Req Null: Stopping Service")
            stop()
        }
        hikeReq?.let {
            if (!it.inProgress) {
                Log.d("LocationService", "Updating Hike Req: in progress," +
                        "not completed, empty path")
                it.inProgress = true
                it.completed = false
                it.traveledPath = emptyList()
            }
        }


        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking hike: ${hikeReq?.name?: "Hike not found"}")
            .setContentText("Location: Null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var count = 5

        locationClient
            .getLocationUpdates(10000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                Log.d("LocationService", "Lat Lng Adding to Path: $latLng")
                hikeReq!!.traveledPath += latLng
                val updatedNotification = notification.setContentText(
                    "Location: (${latLng})"
                )
                notificationManager.notify(1, updatedNotification.build())
                count += 1
                if (count == 6) {
                    count = 0
                    hikeReq?.let {
                        val encryptedHikeReq = apiService.encryptHikeReq(it)
                        try {
                            apiService.apiService.updateHike(encryptedHikeReq)
                        } catch (e: Exception){
                            Log.d("LocationService", "Failed to connect. Trying again later.")
                        }

                    }
                }
                for (client in clients) {
                    hikeReq?.let {
                        client.onLocationReceived(it, latLng)
                    }
                }
            }
            .launchIn(serviceScope)
        startForeground(1, notification.build())
    }

    private fun stop() {
        serviceScope.launch {
            hikeReq?.let {
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d("LocationService", "OnDestroy hit")
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
    fun registerCallback(callback: LocationCallback) {
        clients.add(callback)
    }
    fun unregisterCallback(callback: LocationCallback) {
        clients.remove(callback)
    }
}