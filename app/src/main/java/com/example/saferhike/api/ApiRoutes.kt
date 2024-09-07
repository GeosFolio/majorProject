package com.example.saferhike.api

import com.example.saferhike.viewModels.HikeReq
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiRoutes {
    @GET("hello")
    suspend fun getHelloWorld(): String

    @POST("hikes")
    suspend fun createHike(@Body hikeData: HikeReq): Response<Void>
}