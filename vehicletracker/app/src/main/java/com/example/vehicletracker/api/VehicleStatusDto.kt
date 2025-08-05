package com.example.vehicletracker.api

data class VehicleLocation(
    val latitude: Double?,
    val longitude: Double?
)

data class VehicleStatusDto(
    val deviceId: String?,
    val bluetoothDevice: String?,
    val engineStatus: String, // "ON" or "OFF"
    val speed: Float?,
    val timestamp: String,
    val location: VehicleLocation?
)
