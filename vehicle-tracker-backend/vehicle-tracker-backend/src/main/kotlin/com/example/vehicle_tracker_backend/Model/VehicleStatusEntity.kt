package com.example.vehicle_tracker_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vehicle_status")
data class VehicleStatusEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "device_id", nullable = false)
    val deviceId: String = "",

    @Column(name = "bluetooth_device", nullable = false)
    val bluetoothDevice: String = "",

    @Column(name = "engine_status", nullable = false)
    val engineStatus: String = "",

    @Column(nullable = true)
    val latitude: Double? = null,

    @Column(nullable = true)
    val longitude: Double? = null,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
