package com.example.saferhike.api

import com.example.saferhike.viewModels.HikeReq
import com.example.saferhike.viewModels.UserReq
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiRoutes {

    @GET("hikes")
    suspend fun getUserHikes(@Query("uid") uid: String): Response<List<HikeReq>>

    @POST("hikes")
    suspend fun postHike(@Body hikeData: HikeReq): Response<Void>

    @PUT("hikes")
    suspend fun updateHike(@Body hikeData: HikeReq): Response<Void>

    @DELETE("hikes/{pid}")
    suspend fun deleteHike(@Path("pid") pid: Int): Response<Void>

    @GET("users")
    suspend fun getUser(@Query("uid") uid: String): Response<UserReq>

    @POST("users")
    suspend fun createUser(@Body userData: UserReq): Response<Void>

    @PUT("users")
    suspend fun updateUser(@Body user: UserReq): Response<Void>

    @PUT("start")
    suspend fun startHike(@Body user: HikeReq): Response<Void>
}