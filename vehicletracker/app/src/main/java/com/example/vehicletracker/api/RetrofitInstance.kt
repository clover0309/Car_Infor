package com.example.vehicletracker.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // TODO: 실제 서버 주소로 변경 필요
    private const val BASE_URL = "http://192.168.1.219:8080/"

    val api: VehicleTrackerApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehicleTrackerApi::class.java)
    }
}
