package com.example.vehicletracker.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

import com.example.vehicletracker.api.VehicleStatusDto

// Retrofit 인터페이스
interface VehicleTrackerApi {
    @POST("/api/vehicle/status")
    fun sendVehicleStatus(@Body status: VehicleStatusDto): Call<Void>
}
