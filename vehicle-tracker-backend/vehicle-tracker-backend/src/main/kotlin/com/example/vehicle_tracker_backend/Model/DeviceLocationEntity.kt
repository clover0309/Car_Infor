package com.example.vehicle_tracker_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "device_location")
data class DeviceLocationEntity(
    @Id
    @Column(name = "device_id")
    val deviceId: String = "",

    @Column(name = "device_name", nullable = false)
    val deviceName: String = "",

    @Column(nullable = false)
    val latitude: Double = 0.0,

    @Column(nullable = false)
    val longitude: Double = 0.0,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
