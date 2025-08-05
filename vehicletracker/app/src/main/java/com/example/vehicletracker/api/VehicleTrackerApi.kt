package com.example.vehicletracker.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

import com.example.vehicletracker.api.VehicleStatusDto

// Retrofit 인터페이스
import com.example.vehicletracker.api.DeviceRegisterRequest

interface VehicleTrackerApi {
    @POST("/api/vehicle/status")
    fun sendVehicleStatus(@Body status: VehicleStatusDto): Call<Void>

    @GET("/api/device/exists")
    fun checkDeviceExists(@Query("deviceId") deviceId: String): Call<Boolean>

    @POST("/api/device/register")
    fun registerDevice(@Body device: DeviceRegisterRequest): Call<Void>

    @POST("/api/location")
    fun sendLocation(@Body req: LocationRequest): Call<Void>
}
