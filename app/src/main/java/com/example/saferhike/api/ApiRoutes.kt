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
    suspend fun getUserHikes(@Query("uid") uid: String): Response<List<EncryptedHikeReq>>

    @POST("hikes")
    suspend fun postHike(@Body hikeData: EncryptedHikeReq): Response<Void>

    @PUT("hikes")
    suspend fun updateHike(@Body hikeData: EncryptedHikeReq): Response<Void>

    @DELETE("hikes/{pid}")
    suspend fun deleteHike(@Path("pid") pid: Int): Response<Void>

    @GET("users")
    suspend fun getUser(@Query("uid") uid: String): Response<EncryptedUserReq>

    @POST("users")
    suspend fun createUser(@Body userData: EncryptedUserReq): Response<Void>

    @PUT("users")
    suspend fun updateUser(@Body user: EncryptedUserReq): Response<Void>

    @PUT("hikes/start")
    suspend fun startHike(@Body user: EncryptedHikeReq): Response<Void>

    @GET("hikes/{pid}/paths")
    suspend fun getHikePath(@Path("pid") pid: Int): Response<List<EncryptedLatLng>>
}