package com.example.vehicle_tracker_backend.repository

import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceLocationRepository : JpaRepository<DeviceLocationEntity, Long> {
    fun findTopByDeviceIdOrderByTimestampDesc(deviceId: String): DeviceLocationEntity?
    fun findFirstByOrderByTimestampDescIdxDesc(): DeviceLocationEntity?
}
