package com.example.vehicletracker.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

import com.example.vehicletracker.api.DeviceRegisterRequest
import com.example.vehicletracker.api.DeviceInfoEntity
import com.example.vehicletracker.api.VehicleStatusDto


interface VehicleTrackerApi {
    @POST("/api/vehicle/status")
    fun sendVehicleStatus(@Body status: VehicleStatusDto): Call<Void>

    @GET("api/device/exists")
    suspend fun checkDeviceExists(@Query("deviceId") deviceId: String): Response<Boolean>

    @POST("api/device/register")
    fun registerDevice(@Body device: DeviceRegisterRequest): Call<DeviceInfoEntity>

    @POST("/api/location")
    fun sendLocation(@Body req: LocationRequest): Call<Void>
}
