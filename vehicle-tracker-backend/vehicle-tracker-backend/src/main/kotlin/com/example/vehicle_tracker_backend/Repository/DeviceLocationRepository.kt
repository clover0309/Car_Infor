package com.example.vehicle_tracker_backend.repository

import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DeviceLocationRepository : JpaRepository<DeviceLocationEntity, String> {
    fun findTopByDeviceIdOrderByTimestampDesc(deviceId: String): DeviceLocationEntity?
    @Query("SELECT d FROM DeviceLocationEntity d WHERE d.timestamp = (SELECT MAX(dl.timestamp) FROM DeviceLocationEntity dl)")
    fun findLatestLocation(): DeviceLocationEntity?
}
