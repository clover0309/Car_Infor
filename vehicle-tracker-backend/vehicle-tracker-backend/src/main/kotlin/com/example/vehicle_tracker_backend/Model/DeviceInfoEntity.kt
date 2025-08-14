package com.example.vehicle_tracker_backend.model

import jakarta.persistence.*

@Entity
@Table(name = "device_info")
data class DeviceInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    val idx: Long = 0,
    
    @Column(name = "device_id", nullable = false, unique = true)
    val deviceId: String = "",

    @Column(name = "device_name", nullable = false)
    var deviceName: String = ""
)
