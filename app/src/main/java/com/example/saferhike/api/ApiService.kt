package com.example.saferhike.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class ApiService {
    private val _retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: ApiRoutes by lazy {
        _retrofit.create(ApiRoutes::class.java)
    }

}