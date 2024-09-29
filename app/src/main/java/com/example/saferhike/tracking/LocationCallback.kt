package com.example.saferhike.tracking

import com.example.saferhike.viewModels.HikeReq
import com.google.android.gms.maps.model.LatLng

interface LocationCallback {
    fun onLocationReceived(hikeReq: HikeReq, location: LatLng)
}