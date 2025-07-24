package com.example.vehicle_tracker_backend.Model

import java.time.LocalDateTime
import javax.xml.stream.Location

data class VehicleStatus(
    val deviceId: String,
    val bluetoothDevice: String,
    val engineStatus: String,
    val speed : Double,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val location: Location? = null
)

data class Location(
    val latitude: Double,
    val longitude: Double
)