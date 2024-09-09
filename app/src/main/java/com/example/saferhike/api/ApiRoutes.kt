package com.example.saferhike.api

import com.example.saferhike.viewModels.HikeReq
import com.example.saferhike.viewModels.UserReq
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiRoutes {
    @POST("hikes")
    suspend fun createHike(@Body hikeData: HikeReq): Response<Void>

    @POST("users")
    suspend fun createUser(@Body userData: UserReq): Response<Void>

    @GET("hikes")
    suspend fun getUserHikes(@Query("uid") uid: String): Response<List<HikeReq>>
}