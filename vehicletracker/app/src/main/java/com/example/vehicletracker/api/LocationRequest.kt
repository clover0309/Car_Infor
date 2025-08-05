package com.example.vehicletracker.api

data class LocationRequest(
    val deviceId: String,
    val deviceName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String? = null,
    val speed: Int
)
