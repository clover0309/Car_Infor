package com.example.vehicle_tracker_backend.Model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "device_location")
data class DeviceLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "device_id", nullable = false)
    val deviceId: String,

    @Column(name = "device_name", nullable = false)
    val deviceName: String,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @Column(nullable = false)
    val timestamp: String = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd aHH:mm:ss")),

    @Column(nullable = false)
    val speed: Int = 0
)
