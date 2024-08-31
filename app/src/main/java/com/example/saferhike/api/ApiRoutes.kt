package com.example.saferhike.api

import retrofit2.http.GET

interface ApiRoutes {
    @GET("hello")
    suspend fun getHelloWorld(): String
}