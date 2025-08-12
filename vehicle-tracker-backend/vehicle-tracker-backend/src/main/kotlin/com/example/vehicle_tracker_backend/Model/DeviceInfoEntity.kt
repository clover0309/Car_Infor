package com.example.vehicle_tracker_backend.model

import jakarta.persistence.*

@Entity
@Table(name = "device_info")
data class DeviceInfoEntity(
    @Id
    @Column(name = "device_id", nullable = false, unique = true)
    val deviceId: String = "",

    @Column(name = "device_name", nullable = false)
    var deviceName: String = ""
)
