package com.example.saferhike.composables

import android.location.Location
import com.example.saferhike.viewModels.HikeReq
import com.google.android.gms.maps.model.LatLng

interface LocationCallback {
    fun onLocationReceived(hikeReq: HikeReq, location: LatLng)
}