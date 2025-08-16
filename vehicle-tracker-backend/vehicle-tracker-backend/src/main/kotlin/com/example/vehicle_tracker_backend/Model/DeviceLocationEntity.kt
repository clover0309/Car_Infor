package com.example.vehicle_tracker_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "device_location")
data class DeviceLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    val idx: Long = 0,

    @Column(name = "device_id", nullable = false)
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
